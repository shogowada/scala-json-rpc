package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcError
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.RequestJsonHandler
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServer[JsonSerializerInUse <: JsonSerializer]
(
    val jsonSerializer: JsonSerializerInUse,
    val executionContext: ExecutionContext
) {
  val requestJsonHandlerRepository = new JsonRpcRequestJsonHandlerRepository
  val disposableFunctionRepository = new DisposableFunctionRepository

  def bindAPI[API](api: API): Unit = macro JsonRpcServerMacro.bindAPI[API]

  def receive(json: String): Future[Option[String]] = macro JsonRpcServerMacro.receive
}

object JsonRpcServer {
  type RequestJsonHandler = (String) => Future[Option[String]]

  def apply[JsonSerializerInUse <: JsonSerializer](
      jsonSerializer: JsonSerializerInUse
  )(implicit executionContext: ExecutionContext): JsonRpcServer[JsonSerializerInUse] = {
    new JsonRpcServer(jsonSerializer, executionContext)
  }
}

object JsonRpcServerMacro {
  def bindAPI[API: c.WeakTypeTag](c: blackbox.Context)(api: c.Expr[API]): c.Expr[Unit] = {
    import c.universe._
    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val (serverDefinition, server) = macroUtils.prefixDefinitionAndReference
    val bind = bindAPIImpl[c.type, API](c)(server, None, api)
    c.Expr[Unit](
      q"""
          $serverDefinition
          $bind
          """
    )
  }

  def bindAPIImpl[Context <: blackbox.Context, API: c.WeakTypeTag](c: Context)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API]
  ): c.Expr[Unit] = {
    import c.universe._

    val macroUtils = JsonRpcMacroUtils[c.type](c)

    val requestJsonHandlerRepository = macroUtils.getRequestJsonHandlerRepository(server)

    val apiType: Type = weakTypeOf[API]
    val methodNameToRequestJsonHandlerList = JsonRpcMacroUtils[c.type](c).getJsonRpcAPIMethods(apiType)
        .map((apiMember: MethodSymbol) => createMethodNameToRequestJsonHandler[c.type, API](c)(server, maybeClient, api, apiMember))

    c.Expr[Unit](
      q"""
          Seq(..$methodNameToRequestJsonHandlerList).foreach {
            case (methodName, handler) => $requestJsonHandlerRepository.add(methodName, handler)
          }
          """
    )
  }

  private def createMethodNameToRequestJsonHandler[Context <: blackbox.Context, API](c: blackbox.Context)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[(String, RequestJsonHandler)] = {
    import c.universe._

    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val requestJsonHandlerFactoryMacro = new JsonRpcRequestJsonHandlerFactoryMacro[c.type](c)

    val methodName = macroUtils.getJsonRpcMethodName(method)
    val handler = requestJsonHandlerFactoryMacro.createFromAPIMethod[API](server, maybeClient, api, method)

    c.Expr[(String, RequestJsonHandler)](q"""$methodName -> $handler""")
  }

  def receive(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Future[Option[String]]] = {
    import c.universe._

    val macroUtils = JsonRpcMacroUtils[c.type](c)

    val (serverDefinition, server) = macroUtils.prefixDefinitionAndReference
    val jsonSerializer: Tree = macroUtils.getJsonSerializer(server)
    val requestJsonHandlerRepository = macroUtils.getRequestJsonHandlerRepository(server)
    val executionContext: Tree = macroUtils.getExecutionContext(server)

    val maybeParseErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJsonFromRequestJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.parseError"))
    val maybeInvalidRequestErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJsonFromRequestJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidRequest"))
    val maybeMethodNotFoundErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJsonFromRequestJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.methodNotFound"))

    val maybeErrorJsonOrMethodName = c.Expr[Either[Option[String], String]](
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .toRight($maybeParseErrorJson)
              .right.flatMap(method => {
                if(method.jsonrpc != Constants.JsonRpc) {
                  Left($maybeInvalidRequestErrorJson)
                } else {
                  Right(method.method)
                }
              })
          """
    )

    val maybeErrorJsonOrHandler = c.Expr[Either[Option[String], RequestJsonHandler]](
      q"""
          $maybeErrorJsonOrMethodName
              .right.flatMap((methodName: String) => {
                $requestJsonHandlerRepository.get(methodName)
                  .toRight($maybeMethodNotFoundErrorJson)
              })
          """
    )

    val futureMaybeJson = c.Expr[Future[Option[String]]](
      q"""
          $maybeErrorJsonOrHandler.fold[Future[Option[String]]](
            maybeErrorJson => Future(maybeErrorJson)($executionContext),
            handler => handler($json)
          )
          """
    )

    c.Expr(
      q"""
          ..${macroUtils.imports}
          $serverDefinition
          $futureMaybeJson
          """
    )
  }
}
