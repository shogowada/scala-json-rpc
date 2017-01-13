package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

object JsonRpcModule {

  import com.softwaremill.macwire._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val clientIdFactoryApi: ClientIdFactoryApi = wire[ClientIdFactoryApiImpl]
  lazy val randomNumberSubject: RandomNumberSubject = wire[RandomNumberSubject]

  lazy val jsonRpcServer = {
    val server = JsonRpcServer(UpickleJsonSerializer())
    server.bindApi[ClientIdFactoryApi](clientIdFactoryApi)
    server.bindApi[RandomNumberSubjectApi](randomNumberSubject)
    server
  }

  lazy val randomNumberObserverApiRepository = wire[RandomNumberObserverApiRepository]
}
