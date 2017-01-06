package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros

class JsonRpcServerAndClient[JSON_SERIALIZER <: JsonSerializer](
    val server: JsonRpcServer[JSON_SERIALIZER],
    val client: JsonRpcClient[JSON_SERIALIZER]
) {
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
