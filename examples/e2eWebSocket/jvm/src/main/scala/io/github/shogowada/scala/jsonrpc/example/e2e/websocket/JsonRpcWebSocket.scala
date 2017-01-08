package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.eclipse.jetty.websocket.api.{RemoteEndpoint, Session, WebSocketAdapter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

class JsonRpcWebSocket extends WebSocketAdapter {
  private var serverAndClient: JsonRpcServerAndClient[UpickleJsonSerializer] = _
  private var observerApi: RandomNumberObserverApi = _

  private val observerApiRepository = JsonRpcModule.randomNumberObserverApiRepository

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val remote: RemoteEndpoint = session.getRemote
    val jsonSender: (String) => Unit = (json: String) => Try(remote.sendString(json))

    println(s"New WebSocket connected at ${session.getRemoteAddress}")

    serverAndClient = JsonRpcModule.jsonRpcServerAndClient(jsonSender)
    observerApi = serverAndClient.createApi[RandomNumberObserverApi]

    observerApiRepository.add(observerApi)
  }

  override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
    observerApiRepository.remove(observerApi)

    observerApi = null
    serverAndClient = null

    println(s"WebSocket closed at ${getSession.getRemoteAddress}: $reason")

    super.onWebSocketClose(statusCode, reason)
  }

  override def onWebSocketText(message: String): Unit = {
    serverAndClient.receive(message).onComplete {
      case Success(Some(responseJson: String)) => serverAndClient.send(responseJson)
      case _ =>
    }
  }
}
