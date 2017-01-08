package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder

object JsonRpcModule {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val randomNumberObserverApi: RandomNumberObserverApi = new RandomNumberObserverApiImpl

  lazy val jsonSerializer = UpickleJsonSerializer()

  lazy val jsonRpcServer = {
    val builder = JsonRpcServerBuilder(jsonSerializer)
    builder.bindApi[RandomNumberObserverApi](randomNumberObserverApi)
    builder.build
  }

  def jsonRpcClient(jsonSender: (String) => Unit) = {
    val builder = JsonRpcClientBuilder(jsonSerializer, jsonSender)
    builder.build
  }

  def jsonRpcServerAndClient(jsonSender: (String) => Unit) = {
    JsonRpcServerAndClient(
      jsonRpcServer,
      jsonRpcClient(jsonSender)
    )
  }
}
