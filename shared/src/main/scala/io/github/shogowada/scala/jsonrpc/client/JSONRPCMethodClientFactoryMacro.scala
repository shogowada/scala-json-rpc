package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Models.JSONRPCRequest
import io.github.shogowada.scala.jsonrpc.server.{DisposableFunctionServerFactoryMacro, JSONRPCRequestJSONHandlerFactoryMacro}
import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

class JSONRPCMethodClientFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  lazy val requestJSONHandlerFactoryMacro = new JSONRPCRequestJSONHandlerFactoryMacro[c.type](c)
  lazy val disposableFunctionClientFactoryMacro = new DisposableFunctionClientFactoryMacro[c.type](c)
  lazy val disposableFunctionServerFactoryMacro = new DisposableFunctionServerFactoryMacro[c.type](c)

  def createAsFunction(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRPCMethodName: Tree,
      paramTypes: Seq[Type],
      returnType: Type
  ): Tree = {
    val jsonRPCParameterType: Tree = macroUtils.getJSONRPCParameterType(paramTypes)
    val jsonRPCParameter = getJSONRPCParameter(client, maybeServer, paramTypes)

    def createMethodBody: c.Expr[returnType.type] = {
      if (macroUtils.isJSONRPCNotificationMethod(returnType)) {
        createNotificationMethodBody(
          client,
          jsonRPCParameterType,
          jsonRPCMethodName,
          jsonRPCParameter,
          returnType
        )
      } else if (macroUtils.isJSONRPCRequestMethod(returnType)) {
        createRequestMethodBody(
          client,
          maybeServer,
          jsonRPCParameterType,
          jsonRPCMethodName,
          jsonRPCParameter,
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

  private def getJSONRPCParameter(
      client: Tree,
      maybeServer: Option[Tree],
      paramTypes: Seq[Type]
  ): Tree = {
    def createJSONRPCParameter(paramType: Type, index: Int): Tree = {
      val paramName = getParamName(index)
      if (macroUtils.isDisposableFunctionType(paramType)) {
        val disposableFunctionMethodName: c.Expr[String] = maybeServer
            .map(server => disposableFunctionServerFactoryMacro.getOrCreate(client, server, paramName, paramType))
            .getOrElse(throw new UnsupportedOperationException("To use DisposableFunction, you need to create an API with JSONRPCServerAndClient."))
        q"$disposableFunctionMethodName"
      } else {
        q"$paramName"
      }
    }

    val jsonRPCParameters: Seq[Tree] = paramTypes
        .zipWithIndex
        .map { case (paramType, index) => createJSONRPCParameter(paramType, index) }

    if (jsonRPCParameters.size == 1) {
      val parameter = jsonRPCParameters.head
      q"Tuple1($parameter)"
    } else {
      q"(..$jsonRPCParameters)"
    }
  }

  private def getParamName(index: Int): TermName = {
    TermName(s"param_$index")
  }

  private def createNotificationMethodBody(
      client: Tree,
      jsonRPCParameterType: Tree,
      jsonRPCMethodName: Tree,
      jsonRPCParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = macroUtils.getJSONSerializer(client)
    val send = macroUtils.getSend(client)

    c.Expr[returnType.type](
      q"""
          val notification = JSONRPCNotification[$jsonRPCParameterType](
              jsonrpc = Constants.JSONRPC,
              method = $jsonRPCMethodName,
              params = $jsonRPCParameter
          )
          $jsonSerializer.serialize(notification)
              .foreach((json: String) => $send(json))
          """
    )
  }

  private def createRequestMethodBody(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRPCParameterType: Tree,
      jsonRPCMethodName: Tree,
      jsonRPCParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = macroUtils.getJSONSerializer(client)
    val promisedResponseRepository = macroUtils.getPromisedResponseRepository(client)
    val send = macroUtils.getSend(client)
    val receive = macroUtils.getReceive(client)
    val executionContext = macroUtils.getExecutionContext(client)

    val requestId = TermName("requestId")

    def request(requestId: TermName) = c.Expr[JSONRPCRequest[jsonRPCParameterType.type]](
      q"""
          JSONRPCRequest[$jsonRPCParameterType](
            jsonrpc = Constants.JSONRPC,
            id = $requestId,
            method = $jsonRPCMethodName,
            params = $jsonRPCParameter
          )
          """
    )

    val promisedResponse = TermName("promisedResponse")

    def responseJSONHandler(json: Tree): Tree = {
      val resultType: Type = returnType.typeArgs.head
      createResponseJSONHandler(client, maybeServer, resultType, json)
    }

    c.Expr[returnType.type](
      q"""
          val $requestId = Left(${macroUtils.newUuid})
          $jsonSerializer.serialize(${request(requestId)}) match {
            case Some((requestJSON: String)) => {
              val $promisedResponse = $promisedResponseRepository.addAndGet($requestId)

              $send(requestJSON).onComplete((tried: Try[Option[String]]) => tried match {
                case Success(Some(responseJSON: String)) => $receive(responseJSON)
                case Success(None) =>
                case Failure(throwable) => {
                  $promisedResponseRepository
                      .getAndRemove($requestId)
                      .foreach(promisedResponse => promisedResponse.failure(throwable))
                }
              })($executionContext)

              $promisedResponse.future
                  .map((json: String) => ${responseJSONHandler(q"json")})($executionContext)
            }
            case None => {
              val jsonRPCErrorResponse = JSONRPCErrorResponse(
                jsonrpc = Constants.JSONRPC,
                id = $requestId,
                error = JSONRPCErrors.internalError
              )
              Future.failed(new JSONRPCException(Some(jsonRPCErrorResponse)))
            }
          }
          """
    )
  }

  private def createResponseJSONHandler(
      client: Tree,
      maybeServer: Option[Tree],
      resultType: Type,
      json: Tree
  ): Tree = {
    val jsonSerializer = macroUtils.getJSONSerializer(client)

    def mapResult(resultResponse: Tree): Tree = {
      val result = q"$resultResponse.result"
      if (macroUtils.isDisposableFunctionType(resultType)) {
        maybeServer
            .map(server => disposableFunctionClientFactoryMacro.getOrCreate(server, client, resultType, q"$result"))
            .getOrElse(throw new UnsupportedOperationException("To use an API returning DisposableFunction, you need to create the API with JSONRPCServerAndClient."))
      } else {
        result
      }
    }

    val jsonRPCResultType: Type = macroUtils.getJSONRPCResultType(resultType)

    q"""
        $jsonSerializer.deserialize[JSONRPCResultResponse[$jsonRPCResultType]]($json) match {
          case Some(resultResponse) => ${mapResult(q"resultResponse")}
          case None => {
            val maybeResponse = $jsonSerializer.deserialize[JSONRPCErrorResponse[String]]($json)
            throw new JSONRPCException(maybeResponse)
          }
        }
        """
  }
}
