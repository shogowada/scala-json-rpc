package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.eclipse.jetty.websocket.api.{RemoteEndpoint, Session, WebSocketAdapter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

class JsonRpcWebSocket extends WebSocketAdapter {
  private var connectedWebSocket: JsonRpcConnectedWebSocket = _

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val remote: RemoteEndpoint = session.getRemote
    val sendString: (String) => Unit = (json: String) => Try(remote.sendString(json))

    connectedWebSocket = JsonRpcModule.jsonRpcConnectedWebSocket(sendString)
  }

  override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
    connectedWebSocket.onWebSocketClose()
    connectedWebSocket = null

    super.onWebSocketClose(statusCode, reason)
  }

  override def onWebSocketText(message: String): Unit = {
    connectedWebSocket.onWebSocketText(message)
  }
}

class JsonRpcConnectedWebSocket(
    jsonRpcServerAndClient: JsonRpcServerAndClient[UpickleJsonSerializer]
) {
  def onWebSocketText(message: String): Unit = {
    jsonRpcServerAndClient.receive(message).onComplete {
      case Success(Some(responseJson: String)) => jsonRpcServerAndClient.send(responseJson)
      case _ =>
    }
  }

  def onWebSocketClose(): Unit = {
  }
}
