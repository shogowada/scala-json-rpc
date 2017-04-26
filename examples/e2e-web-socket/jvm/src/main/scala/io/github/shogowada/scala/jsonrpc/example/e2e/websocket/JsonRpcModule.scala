package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

import scala.concurrent.ExecutionContext.Implicits.global

object JsonRpcModule {

  lazy val todoRepositoryAPI = new TodoRepositoryAPIImpl

  lazy val jsonSerializer = UpickleJsonSerializer()

  def createJsonRpcServerAndClient(jsonSender: JsonSender): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val server = JsonRpcServer(jsonSerializer)
    val client = JsonRpcClient(jsonSerializer, jsonSender)
    val serverAndClient = JsonRpcServerAndClient(server, client)

    serverAndClient.bindAPI[TodoRepositoryAPI](todoRepositoryAPI)

    serverAndClient
  }
}
