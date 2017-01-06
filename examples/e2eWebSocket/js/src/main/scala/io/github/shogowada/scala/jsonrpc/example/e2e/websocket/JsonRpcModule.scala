package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder

object JsonRpcModule {

  import com.softwaremill.macwire._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val randomNumberReceiverApi: RandomNumberObserverApi = wire[RandomNumberObserverApiImpl]

  lazy val jsonRpcServer = {
    val builder = JsonRpcServerBuilder(UpickleJsonSerializer())
    builder.bindApi[RandomNumberObserverApi](randomNumberReceiverApi)
    builder.build
  }

  lazy val jsonRpcClient = {
    val builder = JsonRpcClientBuilder(
      UpickleJsonSerializer(),
      (json: String) => ()
    )
    builder.build
  }

  lazy val randomNumberSubscriberApi = jsonRpcClient.createApi[RandomNumberSubjectApi]

  lazy val jsonRpcServerAndClient = JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
}
