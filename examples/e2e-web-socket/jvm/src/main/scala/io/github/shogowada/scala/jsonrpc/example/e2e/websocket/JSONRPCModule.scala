package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JSONRPCServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.client.JSONRPCClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer

import scala.concurrent.ExecutionContext.Implicits.global

object JSONRPCModule {

  lazy val todoRepositoryAPI = new TodoRepositoryAPIImpl

  lazy val jsonSerializer = UpickleJsonSerializer()

  def createJSONRPCServerAndClient(jsonSender: JsonSender): JSONRPCServerAndClient[UpickleJsonSerializer] = {
    val server = JSONRPCServer(jsonSerializer)
    val client = JSONRPCClient(jsonSerializer, jsonSender)
    val serverAndClient = JSONRPCServerAndClient(server, client)

    serverAndClient.bindAPI[TodoRepositoryAPI](todoRepositoryAPI)

    serverAndClient
  }
}
