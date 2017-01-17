package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcError, JsonRpcRequest}
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.RequestJsonHandler
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcHandlerMacroFactory[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val functionServerMacroFactory = new JsonRpcFunctionServerMacroFactory[c.type](c)

  def createHandlerFromApiMethod[API](
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[RequestJsonHandler] = {
    val parameterTypeLists: List[List[Type]] = method.asMethod.paramLists
        .map(parameters => parameters.map(parameter => parameter.typeSignature))

    createHandler(
      server,
      maybeClient,
      q"$api.$method",
      parameterTypeLists,
      method.asMethod.returnType
    )
  }

  def createHandlerFromJsonRpcFunction(
      client: Tree,
      server: Tree,
      jsonRpcFunction: TermName,
      jsonRpcFunctionType: Type
  ): c.Expr[RequestJsonHandler] = {
    val functionType: Type = macroUtils.getFunctionTypeOfJsonRpcFunctionType(jsonRpcFunctionType)
    val paramTypes: Seq[Type] = functionType.typeArgs.init
    val returnType: Type = functionType.typeArgs.last

    createHandler(
      server,
      Some(client),
      q"$jsonRpcFunction.call",
      Seq(paramTypes),
      returnType
    )
  }

  private def createHandler(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      method: c.Tree,
      parameterTypeLists: Seq[Seq[c.Type]],
      returnType: c.Type
  ): c.Expr[RequestJsonHandler] = {
    val macroUtils = JsonRpcMacroUtils[c.type](c)

    val jsonSerializer = macroUtils.getJsonSerializer(server)
    val executionContext = macroUtils.getExecutionContext(server)

    val parameterTypes: Seq[Type] = parameterTypeLists.flatten

    val jsonRpcParameterType: Tree = macroUtils.getJsonRpcParameterType(parameterTypes)

    def arguments(params: TermName): Seq[Tree] = {
      parameterTypes
          .zipWithIndex
          .map { case (parameterType, index) =>
            val fieldName = TermName(s"_${index + 1}")
            val argument = q"$params.$fieldName"
            if (macroUtils.isJsonRpcFunctionType(parameterType)) {
              maybeClient
                  .map(client => functionServerMacroFactory.getOrCreateJsonRpcFunction(server, client, parameterType, argument))
                  .getOrElse(throw new UnsupportedOperationException("To use JsonRpcFunction, you need to bind the API to JsonRpcServerAndClient."))
            } else {
              argument
            }
          }
    }

    val json = TermName("json")
    val params = TermName("params")

    def methodInvocation(params: TermName) = {
      if (parameterTypeLists.isEmpty) {
        q"$method"
      } else {
        q"$method(..${arguments(params)})"
      }
    }

    def notificationHandler = c.Expr[RequestJsonHandler](
      q"""
          ($json: String) => {
            ..${macroUtils.imports}
            $jsonSerializer.deserialize[JsonRpcNotification[$jsonRpcParameterType]]($json)
              .foreach(notification => {
                val $params = notification.params
                ${methodInvocation(params)}
              })
            Future(None)($executionContext)
          }
          """
    )

    def requestHandler = {
      val request = TermName("request")

      val maybeInvalidParamsErrorJson: c.Expr[Option[String]] =
        macroUtils.createMaybeErrorJson(
          server,
          c.Expr[String](q"$json"),
          c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidParams")
        )

      def maybeJsonRpcRequest(json: TermName) = c.Expr[Option[JsonRpcRequest[jsonRpcParameterType.type]]](
        q"""$jsonSerializer.deserialize[JsonRpcRequest[$jsonRpcParameterType]]($json)"""
      )

      c.Expr[RequestJsonHandler](
        q"""
            ($json: String) => {
              ..${macroUtils.imports}
              ${maybeJsonRpcRequest(json)}
                .map(($request: JsonRpcRequest[$jsonRpcParameterType]) => {
                  val $params = $request.params
                  ${methodInvocation(params)}
                    .map((result) => JsonRpcResultResponse(
                      jsonrpc = Constants.JsonRpc,
                      id = $request.id,
                      result = result
                    ))($executionContext)
                    .map((response) => $jsonSerializer.serialize(response))($executionContext)
                })
                .getOrElse(Future($maybeInvalidParamsErrorJson)($executionContext))
            }
            """
      )
    }

    if (macroUtils.isJsonRpcNotificationMethod(returnType)) {
      notificationHandler
    } else {
      requestHandler
    }
  }

  def createDisposeFunctionMethodHandler(
      server: Tree
  ): Tree = {
    val jsonSerializer = macroUtils.getJsonSerializer(server)
    val requestJsonHandlerRepository = macroUtils.getRequestJsonHandlerRepository(server)
    val executionContext = macroUtils.getExecutionContext(server)

    def response(id: Tree) =
      q"""
          JsonRpcResultResponse[Unit](
            jsonrpc = Constants.JsonRpc,
            id = $id,
            result = ()
          )
          """

    val maybeErrorJson = macroUtils.createMaybeErrorJson(
      server,
      c.Expr[String](q"json"),
      c.Expr[JsonRpcError[String]](q"JsonRpcErrors.internalError")
    )

    q"""
        (json: String) => {
          ..${macroUtils.imports}
          val maybeResponse: Option[String] = $jsonSerializer.deserialize[JsonRpcRequest[Tuple1[String]]](json)
            .flatMap(request => {
              val Tuple1(methodName) = request.params
              $requestJsonHandlerRepository.remove(methodName)
              val response = ${response(q"request.id")}
              $jsonSerializer.serialize(response)
            })
            .orElse { $maybeErrorJson }
          Future(maybeResponse)($executionContext)
        }
        """
  }
}
