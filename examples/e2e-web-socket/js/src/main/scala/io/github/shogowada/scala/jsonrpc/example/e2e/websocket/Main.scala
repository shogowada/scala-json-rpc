package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
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
    val webSocket = new dom.WebSocket(webSocketUrl)

    webSocket.onopen = (_: dom.Event) => {
      val jsonRpcServerAndClient = createJsonRpcServerAndClient(webSocket)

      webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
        val message = messageEvent.data.toString
        jsonRpcServerAndClient.receiveAndSend(message)
      }
    }
  }

  private lazy val webSocketUrl: String = {
    val location = dom.window.location
    val protocol = location.protocol match {
      case "http" => "ws"
      case "https" => "wss"
    }
    s"$protocol://${location.host}/jsonrpc"
  }

  private def createJsonRpcServerAndClient(webSocket: WebSocket): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val jsonSerializer = UpickleJsonSerializer()

    val jsonRpcServer = JsonRpcServer(jsonSerializer)

    val jsonSender: JsonSender = (json: String) => {
      Try(webSocket.send(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }
    val jsonRpcClient = JsonRpcClient(jsonSerializer, jsonSender)

    JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
  }
}
