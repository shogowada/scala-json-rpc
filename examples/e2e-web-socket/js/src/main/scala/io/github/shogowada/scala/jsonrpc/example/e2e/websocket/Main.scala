package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.io.IOException

import io.github.shogowada.scala.jsonrpc.JSONRPCServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.client.JSONRPCClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer
import io.github.shogowada.scalajs.reactjs.ReactDOM
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import org.scalajs.dom
import org.scalajs.dom.WebSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.JSApp
import scala.util.{Failure, Try}

object Main extends JSApp {
  override def main(): Unit = {
    val futureWebSocket = createFutureWebSocket()
    val serverAndClient = createServerAndClient(futureWebSocket)

    val mountNode = dom.document.getElementById("mount-node")
    ReactDOM.render(
      <((new TodoListView(serverAndClient.createAPI[TodoRepositoryAPI])) ()).empty,
      mountNode
    )
  }

  private def createFutureWebSocket(): Future[WebSocket] = {
    val promisedWebSocket: Promise[WebSocket] = Promise()
    val webSocket = new dom.WebSocket(webSocketUrl)

    webSocket.onopen = (_: dom.Event) => {
      promisedWebSocket.success(webSocket)
    }

    webSocket.onerror = (event: dom.ErrorEvent) => {
      promisedWebSocket.failure(new IOException(event.message))
    }

    promisedWebSocket.future
  }

  private def webSocketUrl: String = {
    val location = dom.window.location
    val protocol = location.protocol match {
      case "http:" => "ws:"
      case "https:" => "wss:"
    }
    s"$protocol//${location.host}/jsonrpc"
  }

  private def createServerAndClient(futureWebSocket: Future[WebSocket]): JSONRPCServerAndClient[UpickleJsonSerializer] = {
    val jsonSerializer = UpickleJsonSerializer()

    val server = JSONRPCServer(jsonSerializer)

    val jsonSender: JsonSender = (json: String) => {
      futureWebSocket
          .map(webSocket => Try(webSocket.send(json)))
          .flatMap(tried => tried.fold(
            throwable => Future.failed(throwable),
            _ => Future(None)
          ))
    }
    val client = JSONRPCClient(jsonSerializer, jsonSender)

    val serverAndClient = JSONRPCServerAndClient(server, client)

    futureWebSocket.foreach(webSocket => {
      webSocket.onmessage = (event: dom.MessageEvent) => {
        val message = event.data.toString
        serverAndClient.receiveAndSend(message).onComplete {
          case Failure(throwable) => {
            println("Failed to send response", throwable)
          }
          case _ =>
        }
      }
    })

    serverAndClient
  }
}
