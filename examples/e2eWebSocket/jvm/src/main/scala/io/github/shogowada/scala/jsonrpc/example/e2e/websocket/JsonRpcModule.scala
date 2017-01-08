package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder

object JsonRpcModule {

  import com.softwaremill.macwire._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val randomNumberSubject: RandomNumberSubject = wire[RandomNumberSubject]

  lazy val jsonSerializer = UpickleJsonSerializer()

  lazy val jsonRpcServer = {
    val builder = JsonRpcServerBuilder(jsonSerializer)
    builder.bindApi[RandomNumberSubjectApi](randomNumberSubject)
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

  lazy val randomNumberObserverApiRepository = wire[RandomNumberObserverApiRepository]
}
