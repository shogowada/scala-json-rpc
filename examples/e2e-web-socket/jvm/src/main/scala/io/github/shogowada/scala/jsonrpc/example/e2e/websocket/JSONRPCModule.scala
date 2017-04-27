package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JSONRPCServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JSONSender
import io.github.shogowada.scala.jsonrpc.client.JSONRPCClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJSONSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer

import scala.concurrent.ExecutionContext.Implicits.global

object JSONRPCModule {

  lazy val todoRepositoryAPI = new TodoRepositoryAPIImpl

  lazy val jsonSerializer = UpickleJSONSerializer()

  def createJSONRPCServerAndClient(jsonSender: JSONSender): JSONRPCServerAndClient[UpickleJSONSerializer] = {
    val server = JSONRPCServer(jsonSerializer)
    val client = JSONRPCClient(jsonSerializer, jsonSender)
    val serverAndClient = JSONRPCServerAndClient(server, client)

    serverAndClient.bindAPI[TodoRepositoryAPI](todoRepositoryAPI)

    serverAndClient
  }
}
