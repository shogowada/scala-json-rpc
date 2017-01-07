package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServerAndClient[JSON_SERIALIZER <: JsonSerializer](
    val server: JsonRpcServer[JSON_SERIALIZER],
    val client: JsonRpcClient[JSON_SERIALIZER]
) {
  def createApi[API]: API = macro JsonRpcServerAndClientMacro.createApi[API]

  def receive(json: String)(implicit executionContext: ExecutionContext): Future[Option[String]] = {
    val wasJsonRpcResponse: Boolean = client.receive(json)
    if (wasJsonRpcResponse) {
      Future(None)
    } else {
      server.receive(json)
    }
  }
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
}
