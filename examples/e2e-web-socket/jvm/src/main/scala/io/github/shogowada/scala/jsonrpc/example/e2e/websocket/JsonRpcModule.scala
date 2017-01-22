package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

object JsonRpcModule {

  lazy val todoEventSubject = new TodoEventSubject
  lazy val todoRepository = new TodoRepository(todoEventSubject)

  lazy val randomNumberSubject = new RandomNumberSubject

  lazy val jsonSerializer = UpickleJsonSerializer()

  def createJsonRpcServerAndClient(jsonSender: JsonSender): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val server = JsonRpcServer(jsonSerializer)
    val client = JsonRpcClient(jsonSerializer, jsonSender)
    val serverAndClient = JsonRpcServerAndClient(server, client)

    serverAndClient.bindApi[TodoEventSubjectApi](todoEventSubject)
    serverAndClient.bindApi[TodoRepositoryApi](todoRepository)

    serverAndClient
  }
}
