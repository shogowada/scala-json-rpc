package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcNotification, JsonRpcRequest}
import io.github.shogowada.scala.jsonrpc.server.JsonRpcHandlerMacroFactory
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.reflect.macros.blackbox

class JsonRpcMethodClientMacroFactory[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = MacroUtils[c.type](c)
  lazy val handlerMacroFactory = new JsonRpcHandlerMacroFactory[c.type](c)

  def createMethodClientAsFunction(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRpcMethodName: Tree,
      paramTypes: Seq[Type],
      returnType: Type
  ): Tree = {
    def getParamName(index: Int): TermName = TermName(s"param$index")

    val jsonRpcParameterType: Tree = macroUtils.getJsonRpcParameterType(paramTypes)
    val jsonRpcParameters: Seq[Tree] = paramTypes
        .indices
        .map(index => {
          val paramName = getParamName(index)
          val paramType = paramTypes(index)
          if (macroUtils.isJsonRpcFunctionType(paramType)) {
            maybeServer
                .map(server => getOrCreateJsonRpcFunctionParameter(client, server, paramName, paramType))
                .getOrElse(throw new UnsupportedOperationException("To use JsonRpcFunction, you need to create an API with JsonRpcServerAndClient."))
          } else {
            q"$paramName"
          }
        })

    val jsonRpcParameter = if (jsonRpcParameters.size == 1) {
      val parameter = jsonRpcParameters.head
      q"Tuple1($parameter)"
    } else {
      q"(..$jsonRpcParameters)"
    }

    def createMethodBody: c.Expr[returnType.type] = {
      if (macroUtils.isJsonRpcNotificationMethod(returnType)) {
        createNotificationMethodBody(
          client,
          jsonRpcParameterType,
          jsonRpcMethodName,
          jsonRpcParameter,
          returnType
        )
      } else {
        createRequestMethodBody(
          client,
          jsonRpcParameterType,
          jsonRpcMethodName,
          jsonRpcParameter,
          returnType
        )
      }
    }

    val parameterList: Seq[Tree] = paramTypes.indices
        .map(index => {
          val paramType = paramTypes(index)
          q"${getParamName(index)}: $paramType"
        })

    q"""
        (..$parameterList) => {
          ..${macroUtils.imports}
          $createMethodBody
        }
        """
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
