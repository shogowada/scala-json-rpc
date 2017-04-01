package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcRequest
import io.github.shogowada.scala.jsonrpc.server.{DisposableFunctionServerFactoryMacro, JsonRpcRequestJsonHandlerFactoryMacro}
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcMethodClientFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val requestJsonHandlerFactoryMacro = new JsonRpcRequestJsonHandlerFactoryMacro[c.type](c)
  lazy val disposableFunctionClientFactoryMacro = new DisposableFunctionClientFactoryMacro[c.type](c)
  lazy val disposableFunctionServerFactoryMacro = new DisposableFunctionServerFactoryMacro[c.type](c)

  def createAsFunction(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRpcMethodName: Tree,
      paramTypes: Seq[Type],
      returnType: Type
  ): Tree = {
    val jsonRpcParameterType: Tree = macroUtils.getJsonRpcParameterType(paramTypes)
    val jsonRpcParameter = getJsonRpcParameter(client, maybeServer, paramTypes)

    def createMethodBody: c.Expr[returnType.type] = {
      if (macroUtils.isJsonRpcNotificationMethod(returnType)) {
        createNotificationMethodBody(
          client,
          jsonRpcParameterType,
          jsonRpcMethodName,
          jsonRpcParameter,
          returnType
        )
      } else if (macroUtils.isJsonRpcRequestMethod(returnType)) {
        createRequestMethodBody(
          client,
          maybeServer,
          jsonRpcParameterType,
          jsonRpcMethodName,
          jsonRpcParameter,
          returnType
        )
      } else {
        throw new UnsupportedOperationException("JSON RPC method must return either Unit or Future")
      }
    }

    val parameterList: Seq[Tree] = paramTypes
        .zipWithIndex
        .map { case (paramType, index) =>
          q"${getParamName(index)}: $paramType"
        }

    q"""
        (..$parameterList) => {
          ..${macroUtils.imports}
          $createMethodBody
        }
        """
  }

  private def getJsonRpcParameter(
      client: Tree,
      maybeServer: Option[Tree],
      paramTypes: Seq[Type]
  ): Tree = {
    def createJsonRpcParameter(paramType: Type, index: Int): Tree = {
      val paramName = getParamName(index)
      if (macroUtils.isDisposableFunctionType(paramType)) {
        val disposableFunctionMethodName: c.Expr[String] = maybeServer
            .map(server => disposableFunctionServerFactoryMacro.getOrCreate(client, server, paramName, paramType))
            .getOrElse(throw new UnsupportedOperationException("To use DisposableFunction, you need to create an API with JsonRpcServerAndClient."))
        q"$disposableFunctionMethodName"
      } else {
        q"$paramName"
      }
    }

    val jsonRpcParameters: Seq[Tree] = paramTypes
        .zipWithIndex
        .map { case (paramType, index) => createJsonRpcParameter(paramType, index) }

    if (jsonRpcParameters.size == 1) {
      val parameter = jsonRpcParameters.head
      q"Tuple1($parameter)"
    } else {
      q"(..$jsonRpcParameters)"
    }
  }

  private def getParamName(index: Int): TermName = {
    TermName(s"param_$index")
  }

  private def createNotificationMethodBody(
      client: Tree,
      jsonRpcParameterType: Tree,
      jsonRpcMethodName: Tree,
      jsonRpcParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = macroUtils.getJsonSerializer(client)
    val send = macroUtils.getSend(client)

    c.Expr[returnType.type](
      q"""
          val notification = JsonRpcNotification[$jsonRpcParameterType](
              jsonrpc = Constants.JsonRpc,
              method = $jsonRpcMethodName,
              params = $jsonRpcParameter
          )
          $jsonSerializer.serialize(notification)
              .foreach((json: String) => $send(json))
          """
    )
  }

  private def createRequestMethodBody(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRpcParameterType: Tree,
      jsonRpcMethodName: Tree,
      jsonRpcParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = macroUtils.getJsonSerializer(client)
    val promisedResponseRepository = macroUtils.getPromisedResponseRepository(client)
    val send = macroUtils.getSend(client)
    val receive = macroUtils.getReceive(client)
    val executionContext = macroUtils.getExecutionContext(client)

    val requestId = TermName("requestId")

    def request(requestId: TermName) = c.Expr[JsonRpcRequest[jsonRpcParameterType.type]](
      q"""
          JsonRpcRequest[$jsonRpcParameterType](
            jsonrpc = Constants.JsonRpc,
            id = $requestId,
            method = $jsonRpcMethodName,
            params = $jsonRpcParameter
          )
          """
    )

    val promisedResponse = TermName("promisedResponse")

    def responseJsonHandler(json: Tree): Tree = {
      val resultType: Type = returnType.typeArgs.head
      createResponseJsonHandler(client, maybeServer, resultType, json)
    }

    c.Expr[returnType.type](
      q"""
          val $requestId = Left(${macroUtils.newUuid})
          $jsonSerializer.serialize(${request(requestId)}) match {
            case Some((requestJson: String)) => {
              val $promisedResponse = $promisedResponseRepository.addAndGet($requestId)

              $send(requestJson).onComplete((tried: Try[Option[String]]) => tried match {
                case Success(Some(responseJson: String)) => $receive(responseJson)
                case Success(None) =>
                case Failure(throwable) => {
                  $promisedResponseRepository
                      .getAndRemove($requestId)
                      .foreach(promisedResponse => promisedResponse.failure(throwable))
                }
              })($executionContext)

              $promisedResponse.future
                  .map((json: String) => ${responseJsonHandler(q"json")})($executionContext)
            }
            case None => {
              val jsonRpcErrorResponse = JsonRpcErrorResponse(
                jsonrpc = Constants.JsonRpc,
                id = $requestId,
                error = JsonRpcErrors.internalError
              )
              Future.failed(new JsonRpcException(Some(jsonRpcErrorResponse)))
            }
          }
          """
    )
  }

  private def createResponseJsonHandler(
      client: Tree,
      maybeServer: Option[Tree],
      resultType: Type,
      json: Tree
  ): Tree = {
    val jsonSerializer = macroUtils.getJsonSerializer(client)

    def mapResult(resultResponse: Tree): Tree = {
      val result = q"$resultResponse.result"
      if (macroUtils.isDisposableFunctionType(resultType)) {
        maybeServer
            .map(server => disposableFunctionClientFactoryMacro.getOrCreate(server, client, resultType, q"$result"))
            .getOrElse(throw new UnsupportedOperationException("To use an API returning DisposableFunction, you need to create the API with JsonRpcServerAndClient."))
      } else {
        result
      }
    }

    val jsonRpcResultType: Type = macroUtils.getJsonRpcResultType(resultType)

    q"""
        $jsonSerializer.deserialize[JsonRpcResultResponse[$jsonRpcResultType]]($json) match {
          case Some(resultResponse) => ${mapResult(q"resultResponse")}
          case None => {
            val maybeResponse = $jsonSerializer.deserialize[JsonRpcErrorResponse[String]]($json)
            throw new JsonRpcException(maybeResponse)
          }
        }
        """
  }
}
