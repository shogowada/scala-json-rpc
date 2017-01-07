package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import org.scalajs.dom

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
      jsonRpcServerAndClient.receive(messageEvent.data.toString).onComplete {
        case Success(Some(responseJson)) => jsonSender(responseJson)
        case _ =>
      }
    }

    val randomNumberSubjectApi = jsonRpcServerAndClient.createApi[RandomNumberSubjectApi]

    val futureUnregisterer: Future[() => Unit] = randomNumberSubjectApi
        .register((randomNumber) => {
          println(randomNumber)
        })
  }
}
