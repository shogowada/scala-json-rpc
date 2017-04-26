package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JSONRPCServerAndClient
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.eclipse.jetty.websocket.api.{Session, WebSocketAdapter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class JSONRPCWebSocket extends WebSocketAdapter {
  private var serverAndClient: JSONRPCServerAndClient[UpickleJsonSerializer] = _

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val jsonSender: JsonSender = (json: String) => {
      Try(session.getRemote.sendString(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }

    // Create an independent server and client for each WebSocket session.
    // This is to make sure we clean up all the caches (e.g. promised response, etc)
    // on each WebSocket session.
    serverAndClient = JSONRPCModule.createJSONRPCServerAndClient(jsonSender)
  }

  override def onWebSocketText(message: String): Unit = {
    serverAndClient.receiveAndSend(message)
  }
}
