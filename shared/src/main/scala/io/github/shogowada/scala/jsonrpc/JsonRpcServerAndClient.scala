package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServerAndClient[JSON_SERIALIZER <: JsonSerializer](
    val server: JsonRpcServer[JSON_SERIALIZER],
    val client: JsonRpcClient[JSON_SERIALIZER]
) {
  val send: JsonSender = client.send

  def createApi[API]: API = macro JsonRpcServerAndClientMacro.createApi[API]

  def receive(json: String): Future[Option[String]] = macro JsonRpcServerAndClientMacro.receive
}

object JsonRpcServerAndClient {
  def apply[JSON_SERIALIZER <: JsonSerializer](
      server: JsonRpcServer[JSON_SERIALIZER],
      client: JsonRpcClient[JSON_SERIALIZER]
  ) = new JsonRpcServerAndClient(server, client)
}

object JsonRpcServerAndClientMacro {
  def createApi[API: c.WeakTypeTag]
  (c: blackbox.Context)
  : c.Expr[API] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    c.Expr[API](q"${c.prefix.tree}.client.createApi[$apiType]")
  }

  def receive(c: blackbox.Context)(json: c.Expr[String])
  : c.Expr[Future[Option[String]]] = {
    import c.universe._
    val macroUtils = MacroUtils[c.type](c)
    val client: Tree = q"${c.prefix.tree}.client"
    val server: Tree = q"${c.prefix.tree}.server"
    val executionContext: c.Expr[ExecutionContext] = c.Expr(q"$server.executionContext")
    c.Expr[Future[Option[String]]](
      q"""
          ..${macroUtils.imports}
          val wasJsonRpcResponse: Boolean = $client.receive($json)
          if (wasJsonRpcResponse) {
            Future(None)($executionContext)
          } else {
            $server.receive($json)
          }
          """
    )
  }
}
