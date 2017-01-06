package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcServerAndClient
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.eclipse.jetty.websocket.api.{RemoteEndpoint, Session, WebSocketAdapter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
    jsonRpcServer: JsonRpcServer[UpickleJsonSerializer],
    randomNumberSubjectApi: RandomNumberSubjectApi,
    sendString: (String) => Unit
) {
  private val jsonRpcClientBuilder = JsonRpcClientBuilder(UpickleJsonSerializer(), sendString)
  private val jsonRpcClient = jsonRpcClientBuilder.build
  private val jsonRpcServerAndClient = JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)

  private val randomNumberObserverApi = jsonRpcClient.createApi[RandomNumberObserverApi]

  private val futureObserverId: Future[String] = randomNumberObserverApi.getId

  def onWebSocketText(message: String): Unit = {
    jsonRpcServerAndClient.receive(message).onComplete {
      case Success(Some(responseJson: String)) => sendString(responseJson)
      case _ =>
    }
  }

  def onWebSocketClose(): Unit = {
    futureObserverId.foreach(observerId => randomNumberSubjectApi.unregister(observerId))
  }
}
