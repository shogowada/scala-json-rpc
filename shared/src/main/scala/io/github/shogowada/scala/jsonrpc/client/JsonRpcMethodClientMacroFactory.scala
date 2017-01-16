package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcNotification, JsonRpcRequest}
import io.github.shogowada.scala.jsonrpc.server.JsonRpcHandlerMacroFactory
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcMethodClientMacroFactory[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val handlerMacroFactory = new JsonRpcHandlerMacroFactory[c.type](c)

  def createMethodClientAsFunction(
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
    val jsonRpcParameters: Seq[Tree] = paramTypes
        .zipWithIndex
        .map { case (paramType, index) =>
          val paramName = getParamName(index)
          if (macroUtils.isJsonRpcFunctionType(paramType)) {
            maybeServer
                .map(server => getOrCreateJsonRpcFunctionParameter(client, server, paramName, paramType))
                .getOrElse(throw new UnsupportedOperationException("To use JsonRpcFunction, you need to create an API with JsonRpcServerAndClient."))
          } else {
            q"$paramName"
          }
        }

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

  private def getOrCreateJsonRpcFunctionParameter(
      client: Tree,
      server: Tree,
      jsonRpcFunction: TermName,
      jsonRpcFunctionType: Type
  ): Tree = {
    val newMethodName = q"Constants.FunctionMethodNamePrefix + ${macroUtils.newUuid}"

    val bindHandler = macroUtils.getBindHandler(server)
    val handler = handlerMacroFactory.createHandlerFromJsonRpcFunction(client, server, jsonRpcFunction, jsonRpcFunctionType)

    q"""
        val methodName = $newMethodName
        $bindHandler(methodName, $handler)
        methodName
        """
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

    c.Expr[returnType.type](
      q"""
          val $requestId = Left(${macroUtils.newUuid})
          val $promisedResponse = $promisedResponseRepository.addAndGet($requestId)

          $send($requestJson).onComplete((tried: Try[Option[String]]) => tried match {
            case Success(Some(responseJson: String)) => $receive(responseJson)
            case Failure(throwable) => $promisedResponse.failure(throwable)
            case _ =>
          })($executionContext)

          $promisedResponse.future
              .map((json: String) => {
                $jsonSerializer.deserialize[JsonRpcResultResponse[$resultType]](json)
                    .map(resultResponse => resultResponse.result)
                    .getOrElse {
                      val maybeResponse = $jsonSerializer.deserialize[JsonRpcErrorResponse[String]](json)
                      throw new JsonRpcException(maybeResponse)
                    }
              })($executionContext)
          """
    )
  }
}
