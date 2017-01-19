package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.eclipse.jetty.websocket.api.{Session, WebSocketAdapter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class JsonRpcWebSocket extends WebSocketAdapter {
  private var serverAndClient: JsonRpcServerAndClient[UpickleJsonSerializer] = _

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(session.getRemote.sendString(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }

    // Create an independent server and client for each WebSocket session.
    // This is to make sure we clean up all the caches (e.g. promised response, etc)
    // on each WebSocket session.
    val jsonSerializer = JsonRpcModule.jsonSerializer
    val server = JsonRpcServer(jsonSerializer)
    val client = JsonRpcClient(jsonSerializer, jsonSender)
    serverAndClient = JsonRpcServerAndClient(server, client)

    serverAndClient.bindApi[RandomNumberSubjectApi](JsonRpcModule.randomNumberSubject)
  }

  override def onWebSocketText(message: String): Unit = {
    serverAndClient.receive(message)
  }
}
