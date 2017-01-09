package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.eclipse.jetty.websocket.api.{RemoteEndpoint, Session, WebSocketAdapter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

class JsonRpcWebSocket extends WebSocketAdapter {
  private var serverAndClient: JsonRpcServerAndClient[UpickleJsonSerializer] = _
  private var clientApi: ClientApi = _
  private var observerApi: RandomNumberObserverApi = _

  private val randomNumberSubject = JsonRpcModule.randomNumberSubject
  private val observerApiRepository = JsonRpcModule.randomNumberObserverApiRepository

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val remote: RemoteEndpoint = session.getRemote
    val jsonSender: (String) => Unit = (json: String) => Try(remote.sendString(json))

    serverAndClient = JsonRpcModule.jsonRpcServerAndClient(jsonSender)
    clientApi = serverAndClient.createApi[ClientApi]
    observerApi = serverAndClient.createApi[RandomNumberObserverApi]

    clientApi.id.onComplete {
      case Success(id) => observerApiRepository.add(id, observerApi)
      case _ =>
    }
  }

  override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
    val maybeObserverId: Option[String] = observerApiRepository.remove(observerApi)
    maybeObserverId.foreach(id => randomNumberSubject.unregister(id))

    observerApi = null
    clientApi = null
    serverAndClient = null

    super.onWebSocketClose(statusCode, reason)
  }

  override def onWebSocketText(message: String): Unit = {
    serverAndClient.receive(message)
  }
}
