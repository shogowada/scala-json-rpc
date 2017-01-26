package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.io.IOException

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import io.github.shogowada.scalajs.reactjs.ReactDOM
import org.scalajs.dom
import org.scalajs.dom.WebSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.JSApp
import scala.util.Try

object Main extends JSApp {
  override def main(): Unit = {
    val futureServerAndClient = createFutureServerAndClient(createWebSocketUrl)

    val mountNode = dom.document.getElementById("mount-node")
    ReactDOM.render(new TodoListView(
      futureServerAndClient.map(_.createApi[TodoEventSubjectApi]),
      futureServerAndClient.map(_.createApi[TodoRepositoryApi])
    )(TodoListView.Props()), mountNode)
  }

  private def createWebSocketUrl: String = {
    val location = dom.window.location
    val protocol = location.protocol match {
      case "http:" => "ws"
      case "https:" => "wss"
    }
    s"$protocol://${location.host}/jsonrpc"
  }

  private def createFutureServerAndClient(webSocketUrl: String): Future[JsonRpcServerAndClient[UpickleJsonSerializer]] = {
    val promisedJsonRpcServerAndClient: Promise[JsonRpcServerAndClient[UpickleJsonSerializer]] = Promise()

    val webSocket = new dom.WebSocket(webSocketUrl)

    webSocket.onopen = (_: dom.Event) => {
      val serverAndClient = createServerAndClient(webSocket)

      webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
        val message = messageEvent.data.toString
        serverAndClient.receiveAndSend(message)
      }

      promisedJsonRpcServerAndClient.success(serverAndClient)
    }

    webSocket.onerror = (event: dom.ErrorEvent) => {
      promisedJsonRpcServerAndClient.failure(new IOException(event.message))
    }

    promisedJsonRpcServerAndClient.future
  }

  private def createServerAndClient(webSocket: WebSocket): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val jsonSerializer = UpickleJsonSerializer()

    val server = JsonRpcServer(jsonSerializer)

    val jsonSender: JsonSender = (json: String) => {
      Try(webSocket.send(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }
    val client = JsonRpcClient(jsonSerializer, jsonSender)

    JsonRpcServerAndClient(server, client)
  }
}
