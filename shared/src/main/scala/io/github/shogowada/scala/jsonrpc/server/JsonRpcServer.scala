package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.communicators.{JsonReceiver, JsonSender}
import io.github.shogowada.scala.jsonrpc.models.Models
import io.github.shogowada.scala.jsonrpc.models.Models._
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

import scala.util.{Failure, Success}

class JsonRpcRequestServer[PARAMS, ERROR, RESULT]
(
    jsonSender: JsonSender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer,
    val methodName: String,
    method: JsonRpcRequestMethod[PARAMS, ERROR, RESULT]
) extends JsonReceiver {

  override def receive(json: String): Unit = {
    jsonDeserializer.deserialize[JsonRpcRequest[PARAMS]](json)
        .filter(request => request.jsonrpc == Models.jsonRpc)
        .filter(request => request.method == methodName)
        .foreach(handle)
  }

  private def handle(request: JsonRpcRequest[PARAMS]): Unit = {
    method(request).onComplete {
      case Success(result) => send(result)
      case Failure(error) => send(JsonRpcResponse(request.id, JsonRpcErrors.internalError.copy(data = Some(error.toString))))
    }
  }

  private def send[T](response: T): Unit = {
    jsonSerializer.serialize(response)
        .foreach(jsonSender.send)
  }
}

class JsonRpcNotificationServer[PARAMS]
(
    jsonDeserializer: JsonDeserializer,
    val methodName: String,
    method: JsonRpcNotificationMethod[PARAMS]
) extends JsonReceiver {
  override def receive(json: String): Unit = {
    jsonDeserializer.deserialize[JsonRpcNotification[PARAMS]](json)
        .filter(notification => notification.jsonrpc == Models.jsonRpc)
        .filter(notification => notification.method == methodName)
        .foreach(method)
  }
}
