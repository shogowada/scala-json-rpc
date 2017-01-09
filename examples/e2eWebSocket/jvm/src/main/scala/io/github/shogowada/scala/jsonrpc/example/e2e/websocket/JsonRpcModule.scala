package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

import scala.concurrent.Future

object JsonRpcModule {

  import com.softwaremill.macwire._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val clientIdFactoryApi: ClientIdFactoryApi = wire[ClientIdFactoryApiImpl]
  lazy val randomNumberSubject: RandomNumberSubject = wire[RandomNumberSubject]

  lazy val jsonSerializer = UpickleJsonSerializer()

  lazy val jsonRpcServer = {
    val server = JsonRpcServer(jsonSerializer)
    server.bindApi[ClientIdFactoryApi](clientIdFactoryApi)
    server.bindApi[RandomNumberSubjectApi](randomNumberSubject)
    server
  }

  def jsonRpcClient(jsonSender: (String) => Unit) = {
    JsonRpcClient(jsonSerializer, (json) => {
      jsonSender(json)
      Future(None)
    })
  }

  def jsonRpcServerAndClient(jsonSender: (String) => Unit) = {
    JsonRpcServerAndClient(
      jsonRpcServer,
      jsonRpcClient(jsonSender)
    )
  }

  lazy val randomNumberObserverApiRepository = wire[RandomNumberObserverApiRepository]
}
