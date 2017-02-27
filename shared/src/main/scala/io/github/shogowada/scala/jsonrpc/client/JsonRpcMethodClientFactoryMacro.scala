package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcNotification, JsonRpcRequest}
import io.github.shogowada.scala.jsonrpc.server.{JsonRpcFunctionServerFactoryMacro, JsonRpcRequestJsonHandlerFactoryMacro}
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcMethodClientFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val requestJsonHandlerFactoryMacro = new JsonRpcRequestJsonHandlerFactoryMacro[c.type](c)
  lazy val jsonRpcFunctionClientFactoryMacro = new JsonRpcFunctionClientFactoryMacro[c.type](c)
  lazy val jsonRpcFunctionServerFactoryMacro = new JsonRpcFunctionServerFactoryMacro[c.type](c)

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
      if (macroUtils.isJsonRpcFunctionType(paramType)) {
        val jsonRpcFunctionMethodName: c.Expr[String] = maybeServer
            .map(server => jsonRpcFunctionServerFactoryMacro.getOrCreate(client, server, paramName, paramType))
            .getOrElse(throw new UnsupportedOperationException("To use JsonRpcFunction, you need to create an API with JsonRpcServerAndClient."))
        q"$jsonRpcFunctionMethodName"
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

    val notification = c.Expr[JsonRpcNotification[jsonRpcParameterType.type]](
      q"""
          JsonRpcNotification[$jsonRpcParameterType](
            jsonrpc = Constants.JsonRpc,
            method = $jsonRpcMethodName,
            params = $jsonRpcParameter
          )
          """
    )

    val notificationJson = c.Expr[String](q"$jsonSerializer.serialize($notification).get")

    c.Expr[returnType.type](q"$send($notificationJson)")
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

    val resultType: Type = returnType.typeArgs.head

    val requestId = TermName("requestId")

    val request = c.Expr[JsonRpcRequest[jsonRpcParameterType.type]](
      q"""
          JsonRpcRequest[$jsonRpcParameterType](
            jsonrpc = Constants.JsonRpc,
            id = $requestId,
            method = $jsonRpcMethodName,
            params = $jsonRpcParameter
          )
          """
    )

    val requestJson = c.Expr[String](q"""$jsonSerializer.serialize($request).get""")

    val promisedResponse = TermName("promisedResponse")

    def createSendJsonFailureHandler(throwable: TermName): Tree = {
      q"""
          $promisedResponseRepository
              .getAndRemove($requestId)
              .foreach(promisedResponse => promisedResponse.failure($throwable))
          """
    }

    def responseJsonHandler(json: TermName): Tree = createResponseJsonHandler(client, maybeServer, resultType, json)

    c.Expr[returnType.type](
      q"""
          val $requestId = Left(${macroUtils.newUuid})
          val $promisedResponse = $promisedResponseRepository.addAndGet($requestId)

          $send($requestJson).onComplete((tried: Try[Option[String]]) => tried match {
            case Success(Some(responseJson: String)) => $receive(responseJson)
            case Failure(throwable) => ${createSendJsonFailureHandler(TermName("throwable"))}
            case _ =>
          })($executionContext)

          $promisedResponse.future
              .map((json: String) => ${responseJsonHandler(TermName("json"))})($executionContext)
          """
    )
  }

  private def createResponseJsonHandler(
      client: Tree,
      maybeServer: Option[Tree],
      resultType: Type,
      json: TermName
  ): Tree = {
    val jsonSerializer = macroUtils.getJsonSerializer(client)

    def mapResult(resultResponse: Tree): Tree = {
      val result = q"$resultResponse.result"
      if (macroUtils.isJsonRpcFunctionType(resultType)) {
        maybeServer
            .map(server => jsonRpcFunctionClientFactoryMacro.getOrCreate(server, client, resultType, q"$result"))
            .getOrElse(throw new UnsupportedOperationException("To use an API returning JsonRpcFunction, you need to create the API with JsonRpcServerAndClient."))
      } else {
        result
      }
    }

    val resultResponse = q"resultResponse"
    val jsonRpcResultType: Type = macroUtils.getJsonRpcResultType(resultType)

    q"""
        $jsonSerializer.deserialize[JsonRpcResultResponse[$jsonRpcResultType]]($json)
            .map($resultResponse => ${mapResult(resultResponse)})
            .getOrElse {
              val maybeResponse = $jsonSerializer.deserialize[JsonRpcErrorResponse[String]](json)
              throw new JsonRpcException(maybeResponse)
            }
        """
  }
}
