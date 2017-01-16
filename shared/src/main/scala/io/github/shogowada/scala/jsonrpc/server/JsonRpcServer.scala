package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcError
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.Handler
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServer[JSON_SERIALIZER <: JsonSerializer]
(
    val jsonSerializer: JSON_SERIALIZER,
    val executionContext: ExecutionContext
) {
  val lock = new Object()

  var methodNameToHandlerMap: Map[String, Handler] = Map()

  def bindHandler(methodName: String, handler: Handler): Unit = {
    lock.synchronized(methodNameToHandlerMap = methodNameToHandlerMap + (methodName -> handler))
  }

  def unbindMethod(methodName: String): Unit = {
    lock.synchronized(methodNameToHandlerMap = methodNameToHandlerMap - methodName)
  }

  def bindApi[API](api: API): Unit = macro JsonRpcServerMacro.bindApi[API]

  def receive(json: String): Future[Option[String]] = macro JsonRpcServerMacro.receive
}

object JsonRpcServer {
  type Handler = (String) => Future[Option[String]]

  def apply[JSON_SERIALIZER <: JsonSerializer](jsonSerializer: JSON_SERIALIZER)(implicit executionContext: ExecutionContext) = {
    new JsonRpcServer(jsonSerializer, executionContext)
  }
}

object JsonRpcServerMacro {
  def bindApi[API: c.WeakTypeTag](c: blackbox.Context)(api: c.Expr[API]): c.Expr[Unit] = {
    bindApiImpl[c.type, API](c)(c.prefix.tree, None, api)
  }

  def bindApiImpl[CONTEXT <: blackbox.Context, API: c.WeakTypeTag](c: CONTEXT)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API]
  ): c.Expr[Unit] = {
    import c.universe._

    val macroUtils = JsonRpcMacroUtils[c.type](c)

    val bindHandler = macroUtils.getBindHandler(server)

    val apiType: Type = weakTypeOf[API]
    val methodNameToHandlerList = JsonRpcMacroUtils[c.type](c).getJsonRpcApiMethods(apiType)
        .map((apiMember: MethodSymbol) => createMethodNameToHandler[c.type, API](c)(server, maybeClient, api, apiMember))

    c.Expr[Unit](
      q"""
          Map(..$methodNameToHandlerList).foreach {
            case (methodName, handler) => $bindHandler(methodName, handler)
          }
          """
    )
  }

  private def createMethodNameToHandler[CONTEXT <: blackbox.Context, API](c: blackbox.Context)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[(String, Handler)] = {
    import c.universe._

    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val handlerMacroFactory = new JsonRpcHandlerMacroFactory[c.type](c)

    val methodName = macroUtils.getJsonRpcMethodName(method)
    val handler = handlerMacroFactory.createHandlerFromApiMethod[API](server, maybeClient, api, method)

    c.Expr[(String, Handler)](q"""$methodName -> $handler""")
  }

  def receive(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Future[Option[String]]] = {
    import c.universe._

    val macroUtils = JsonRpcMacroUtils[c.type](c)

    val server = c.prefix.tree
    val jsonSerializer: Tree = q"$server.jsonSerializer"
    val methodNameToHandlerMap: Tree = q"$server.methodNameToHandlerMap"
    val executionContext: Tree = q"$server.executionContext"

    val maybeParseErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.parseError"))
    val maybeInvalidRequestErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidRequest"))
    val maybeMethodNotFoundErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.methodNotFound"))

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

    val maybeErrorJsonOrHandler = c.Expr[Either[Option[String], Handler]](
      q"""
          $maybeErrorJsonOrMethodName
              .right.flatMap((methodName: String) => {
                $methodNameToHandlerMap.get(methodName)
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
          $futureMaybeJson
          """
    )
  }
}
