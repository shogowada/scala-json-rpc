package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder

import scala.concurrent.Future

object JsonRpcModule {

  import com.softwaremill.macwire._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val randomNumberObserverApi: RandomNumberObserverApi = wire[RandomNumberObserver]

  lazy val jsonSerializer = UpickleJsonSerializer()

  lazy val jsonRpcServer = {
    val builder = JsonRpcServerBuilder(jsonSerializer)
    builder.bindApi[RandomNumberObserverApi](randomNumberObserverApi)
    builder.build
  }

  def jsonRpcClient(jsonSender: (String) => Unit) = {
    val builder = JsonRpcClientBuilder(jsonSerializer, (json) => {
      jsonSender(json)
      Future(None)
    })
    builder.build
  }

  def jsonRpcServerAndClient(jsonSender: (String) => Unit) = {
    JsonRpcServerAndClient(
      jsonRpcServer,
      jsonRpcClient(jsonSender)
    )
  }
}
