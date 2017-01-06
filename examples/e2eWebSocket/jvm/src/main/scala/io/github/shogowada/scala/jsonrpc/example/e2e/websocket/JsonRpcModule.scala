package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder

object JsonRpcModule {

  import com.softwaremill.macwire._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val randomNumberSubjectApi: RandomNumberSubjectApi = wire[RandomNumberSubjectApiImpl]

  lazy val jsonRpcServer = {
    val builder = JsonRpcServerBuilder(UpickleJsonSerializer())
    builder.bindApi(randomNumberSubjectApi)
    builder.build
  }

  lazy val jsonRpcClient = {
    val builder = JsonRpcClientBuilder(
      UpickleJsonSerializer(),
      (json: String) => ()
    )
    builder.build
  }

  lazy val randomNumberObserverApi = jsonRpcClient.createApi[RandomNumberObserverApi]

  lazy val jsonRpcServerAndClient = JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)

  def jsonRpcConnectedWebSocket(sendString: (String) => Unit) = wire[JsonRpcConnectedWebSocket]
}
