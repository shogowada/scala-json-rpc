package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcMethod
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.Handler
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServer[JSON_SERIALIZER <: JsonSerializer]
(
    val methodNameToHandlerMap: Map[String, Handler],
    val jsonSerializer: JSON_SERIALIZER
) {
  def receive(json: String): Future[Option[String]] = macro JsonRpcServerMacro.receive
}

object JsonRpcServer {
  type Handler = (String) => Future[Option[String]]
}

object JsonRpcServerMacro {
  def receive
  (c: blackbox.Context)
  (json: c.Expr[String])
  : c.Expr[Future[Option[String]]] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"
    val methodNameToHandlerMap: Tree = q"${c.prefix.tree}.methodNameToHandlerMap"

    val maybeJsonRpcMethod = c.Expr[Option[JsonRpcMethod]](
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .filter(method => method.jsonrpc == Constants.JsonRpc)
          """
    )

    val maybeHandler = c.Expr[Option[Handler]](
      q"""
          $maybeJsonRpcMethod
              .flatMap((jsonRpcMethod: JsonRpcMethod) => {
                $methodNameToHandlerMap.get(jsonRpcMethod.method)
              })
          """
    )

    val futureMaybeJson = c.Expr[Future[Option[String]]](
      q"""
          $maybeHandler.map(handler => handler($json))
            .getOrElse(Future(None))
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
