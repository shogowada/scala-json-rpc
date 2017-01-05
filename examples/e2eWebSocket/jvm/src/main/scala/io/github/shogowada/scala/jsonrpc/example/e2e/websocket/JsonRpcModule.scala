package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder

object JsonRpcModule {

  import com.softwaremill.macwire._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val randomNumberSubscriberApi: RandomNumberSubscriberApi = wire[RandomNumberSubscriberApiImpl]

  lazy val jsonRpcServer = {
    val builder = JsonRpcServerBuilder(UpickleJsonSerializer())
    builder.bindApi[RandomNumberSubscriberApi](randomNumberSubscriberApi)
    builder.build
  }

  lazy val jsonRpcClient = {
    val builder = JsonRpcClientBuilder(
      UpickleJsonSerializer(),
      (json: String) => ()
    )
    builder.build
  }

  lazy val randomNumberReceiverApi = jsonRpcClient.createApi[RandomNumberReceiverApi]
}
