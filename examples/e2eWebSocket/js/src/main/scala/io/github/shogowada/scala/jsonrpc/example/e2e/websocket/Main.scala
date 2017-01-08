package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.JSApp
import scala.util.{Success, Try}

object Main extends JSApp {
  override def main(): Unit = {
    val webSocket = new dom.WebSocket("ws://localhost:8080/jsonrpc")

    val jsonSender: (String) => Unit = (json: String) => {
      Try(webSocket.send(json))
    }
    val jsonRpcServerAndClient = JsonRpcModule.jsonRpcServerAndClient(jsonSender)

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRpcServerAndClient.receive(message).onComplete {
        case Success(Some(responseJson: String)) => jsonRpcServerAndClient.send(responseJson)
        case _ =>
      }
    }

    webSocket.onopen = (_: dom.Event) => {
      val subjectApi = jsonRpcServerAndClient.createApi[RandomNumberSubjectApi]
      val futureObserverId: Future[String] = subjectApi.createObserverId()

      futureObserverId.onComplete {
        case Success(id) => {
          val observerApi = JsonRpcModule.randomNumberObserverApi
          observerApi.setId(id)
          subjectApi.register(id)
        }
        case _ =>
      }
    }
  }
}
