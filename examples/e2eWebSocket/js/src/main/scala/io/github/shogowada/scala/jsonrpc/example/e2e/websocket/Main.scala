package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.scalajs.dom
import org.scalajs.dom.WebSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.JSApp
import scala.util.{Success, Try}

object Main extends JSApp {
  override def main(): Unit = {
    val webSocket = new dom.WebSocket("ws://localhost:8080/jsonrpc")

    val jsonRpcServerAndClient = createJsonRpcServerAndClient(webSocket)

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRpcServerAndClient.receive(message)
    }

    val promisedClientId: Promise[String] = Promise()
    jsonRpcServerAndClient.bindApi[ClientApi](new ClientApiImpl(promisedClientId))
    jsonRpcServerAndClient.bindApi[RandomNumberObserverApi](new RandomNumberObserverApiImpl)

    webSocket.onopen = (_: dom.Event) => {
      val clientIdFactoryApi = jsonRpcServerAndClient.createApi[ClientIdFactoryApi]
      val futureClientId: Future[String] = clientIdFactoryApi.create() // Wait until connection is open to use client APIs

      val subjectApi = jsonRpcServerAndClient.createApi[RandomNumberSubjectApi]

      futureClientId.onComplete {
        case Success(id) => {
          promisedClientId.success(id)
          subjectApi.register(id)
        }
        case _ =>
      }
    }
  }

  private def createJsonRpcServerAndClient(webSocket: WebSocket): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val jsonSerializer = UpickleJsonSerializer()

    val jsonRpcServer = JsonRpcServer(jsonSerializer)

    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(webSocket.send(json)).failed.toOption
          .map(throwable => Future.failed(throwable))
          .getOrElse(Future(None))
    }
    val jsonRpcClient = JsonRpcClient(jsonSerializer, jsonSender)

    JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
  }
}
