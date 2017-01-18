package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.scalajs.dom
import org.scalajs.dom.WebSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.JSApp
import scala.util.Try

object Main extends JSApp {
  override def main(): Unit = {
    val webSocket = new dom.WebSocket("ws://localhost:8080/jsonrpc")

    var jsonRpcServerAndClient: JsonRpcServerAndClient[UpickleJsonSerializer] = null

    webSocket.onopen = (_: dom.Event) => {
      jsonRpcServerAndClient = createJsonRpcServerAndClient(webSocket)

      val subjectApi = jsonRpcServerAndClient.createApi[RandomNumberSubjectApi]

      subjectApi.register((randomNumber: Int) => {
        println(randomNumber)
        Future() // Making sure server knows if it was successful
      })
    }

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRpcServerAndClient.receive(message)
    }

    webSocket.onclose = (_: dom.CloseEvent) => {
      jsonRpcServerAndClient = null
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
