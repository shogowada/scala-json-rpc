package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.communicators.{Receiver, Sender}
import io.github.shogowada.scala.jsonrpc.models.{JsonRpcErrors, JsonRpcNotification, JsonRpcRequest, JsonRpcResponse}
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

class JsonRpcServer
(
    sender: Sender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer
) extends Receiver {
  def bindApi[T](api: T): Unit = bindApi(() => api)

  def bindApi[T](apiFactory: () => T): Unit = {
    throw new UnsupportedOperationException
  }

  override def receive(json: String): Unit = {
    val maybeRequest = jsonDeserializer.deserialize[JsonRpcRequest](json)
    if (maybeRequest.isDefined) {
      handleRequest(maybeRequest.get)
      return
    }
    val maybeNotification = jsonDeserializer.deserialize[JsonRpcNotification](json)
    if (maybeNotification.isDefined) {
      handleNotification(maybeNotification.get)
      return
    }
    jsonSerializer.serialize(
      JsonRpcResponse(
        jsonrpc = "2.0",
        id = null,
        result = None,
        error = Option(JsonRpcErrors.InvalidRequest)
      )
    ).foreach(sender.send)
  }

  private def handleRequest(request: JsonRpcRequest): Unit = {
    throw new UnsupportedOperationException
  }

  private def handleNotification(notification: JsonRpcNotification): Unit = {
    throw new UnsupportedOperationException
  }
}
