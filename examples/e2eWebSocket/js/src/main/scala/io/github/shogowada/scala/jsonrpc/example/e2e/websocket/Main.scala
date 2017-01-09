package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.JSApp
import scala.util.{Success, Try}

object Main extends JSApp {
  override def main(): Unit = {
    val webSocket = new dom.WebSocket("ws://localhost:8080/jsonrpc")

    val promisedObserverId: Promise[String] = Promise()
    val randomNumberObserverApi = new RandomNumberObserverApiImpl(promisedObserverId)

    val jsonSerializer = UpickleJsonSerializer()
    val jsonRpcServerBuilder = JsonRpcServerBuilder(jsonSerializer)
    jsonRpcServerBuilder.bindApi[RandomNumberObserverApi](randomNumberObserverApi)
    val jsonRpcServer = jsonRpcServerBuilder.build

    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(webSocket.send(json))
      Future(None)
    }
    val jsonRpcClientBuilder = JsonRpcClientBuilder(jsonSerializer, jsonSender)
    val jsonRpcClient = jsonRpcClientBuilder.build

    val jsonRpcServerAndClient = JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRpcServerAndClient.receive(message)
    }

    webSocket.onopen = (_: dom.Event) => {
      val subjectApi = jsonRpcServerAndClient.createApi[RandomNumberSubjectApi]
      val futureObserverId: Future[String] = subjectApi.createObserverId()

      futureObserverId.onComplete {
        case Success(id) => {
          promisedObserverId.success(id)
          subjectApi.register(id)
        }
        case _ =>
      }
    }
  }
}
