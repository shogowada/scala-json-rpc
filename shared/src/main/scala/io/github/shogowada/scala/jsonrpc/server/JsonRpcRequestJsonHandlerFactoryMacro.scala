package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcError
import io.github.shogowada.scala.jsonrpc.client.JsonRpcFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.RequestJsonHandler
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcRequestJsonHandlerFactoryMacro[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val jsonRpcFunctionClientFactoryMacro = new JsonRpcFunctionClientFactoryMacro[c.type](c)
  lazy val jsonRpcFunctionServerFactoryMacro = new JsonRpcFunctionServerFactoryMacro[c.type](c)

  def createFromApiMethod[API](
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[RequestJsonHandler] = {
    val parameterTypeLists: List[List[Type]] = method.asMethod.paramLists
        .map(parameters => parameters.map(parameter => parameter.typeSignature))

    create(
      server,
      maybeClient,
      q"$api.$method",
      parameterTypeLists,
      method.asMethod.returnType
    )
  }

  def createFromJsonRpcFunction(
      client: Tree,
      server: Tree,
      jsonRpcFunction: TermName,
      jsonRpcFunctionType: Type
  ): c.Expr[RequestJsonHandler] = {
    val paramTypes: Seq[Type] = jsonRpcFunctionType.typeArgs.init
    val returnType: Type = jsonRpcFunctionType.typeArgs.last

    create(
      server,
      Some(client),
      q"$jsonRpcFunction",
      Seq(paramTypes),
      returnType
    )
  }

  private def create(
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
                  .map(client => jsonRpcFunctionClientFactoryMacro.getOrCreate(server, client, parameterType, argument))
                  .getOrElse(throw new UnsupportedOperationException("To use JsonRpcFunction, you need to bind the API to JsonRpcServerAndClient."))
            } else {
              argument
            }
          }
    }

    val json = TermName("json")
    val params = TermName("params")

    val returnsJsonRpcFunction = returnType.typeArgs
        .headOption
        .exists(macroUtils.isJsonRpcFunctionType)

    def methodInvocation(params: TermName) = {
      val realMethodInvocation = if (parameterTypeLists.isEmpty) {
        q"$method"
      } else {
        q"$method(..${arguments(params)})"
      }

      if (returnsJsonRpcFunction) {
        def jsonRpcFunctionServer(result: TermName): c.Expr[String] = maybeClient
            .map(client => jsonRpcFunctionServerFactoryMacro.getOrCreate(client, server, result, returnType))
            .getOrElse {
              throw new UnsupportedOperationException("To return JsonRpcFunction, you need to implement an API with JsonRpcServerAndClient.")
            }
        q"""
            $realMethodInvocation
                .map(jsonRpcFunction => ${jsonRpcFunctionServer(TermName("jsonRpcFunction"))})($executionContext)
            """
      } else {
        realMethodInvocation
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

      c.Expr[RequestJsonHandler](
        q"""
            ($json: String) => {
              ..${macroUtils.imports}
              $jsonSerializer.deserialize[JsonRpcRequest[$jsonRpcParameterType]]($json)
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
      server: Tree,
      client: Tree
  ): Tree = {
    val jsonSerializer = macroUtils.getJsonSerializer(server)
    val jsonRpcFunctionMethodNameRepository = macroUtils.getJsonRpcFunctionMethodNameRepository(client)
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
              $jsonRpcFunctionMethodNameRepository.remove(methodName)
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
