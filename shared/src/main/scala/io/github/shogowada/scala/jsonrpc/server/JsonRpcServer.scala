package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.communicators.{JsonReceiver, JsonSender}
import io.github.shogowada.scala.jsonrpc.models.Models
import io.github.shogowada.scala.jsonrpc.models.Models._
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

import scala.util.{Failure, Success}

class JsonRpcServer
(
    jsonRpcMethodRepository: JsonRpcMethodRepository,
    jsonSender: JsonSender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer
) extends JsonReceiver {

  def bindMethod(name: String, method: JsonRpcRequestMethod): Unit = {
    jsonRpcMethodRepository.bind(name, method)
  }

  def bindMethod(name: String, method: JsonRpcNotificationMethod): Unit = {
    jsonRpcMethodRepository.bind(name, method)
  }

  override def receive(json: String): Unit = {
    if (maybeHandleRequest(json) || maybeHandleNotification(json)) {
      return
    }
    send(JsonRpcResponse(JsonRpcErrors.invalidRequest))
  }

  private def maybeHandleRequest(json: String): Boolean = {
    val maybeRequest = jsonDeserializer.deserialize[JsonRpcRequest](json)
        .filter(response => response.jsonrpc == Models.jsonRpc)
    val maybeMethod = maybeRequest
        .flatMap(request => jsonRpcMethodRepository.getRequestMethod(request.method))

    (maybeRequest, maybeMethod) match {
      case (None, _) => false
      case (Some(request), None) =>
        sendMethodNotFound(request.id, request.method)
        true
      case (Some(request), Some(method)) =>
        method(request).onComplete {
          case Success(response) => send(response)
          case Failure(error) => send(JsonRpcResponse(request.id, JsonRpcErrors.internalError.copy(data = Some(error.toString))))
        }
        true
    }
  }

  private def maybeHandleNotification(json: String): Boolean = {
    val maybeNotification = jsonDeserializer.deserialize[JsonRpcNotification](json)
        .filter(response => response.jsonrpc == Models.jsonRpc)
    val maybeMethod = maybeNotification
        .flatMap(notification => jsonRpcMethodRepository.getNotificationMethod(notification.method))

    (maybeNotification, maybeMethod) match {
      case (None, _) => false
      case (Some(notification), None) =>
        sendMethodNotFound(notification.method)
        true
      case (Some(notification), Some(method)) =>
        method(notification)
        true
    }
  }

  private def sendMethodNotFound(method: String): Unit = {
    send(JsonRpcResponse(
      JsonRpcErrors.methodNotFound.copy(data = Some(s"Notification method '$method' was not found."))
    ))
  }

  private def sendMethodNotFound(id: Id, method: String): Unit = {
    send(JsonRpcResponse(
      id,
      JsonRpcErrors.methodNotFound.copy(data = Some(s"Request method '$method' was not found."))
    ))
  }

  private def send[T](response: T): Unit = {
    jsonSerializer.serialize(response)
        .foreach(jsonSender.send)
  }
}

object JsonRpcServer {
  def apply(jsonSender: JsonSender, jsonSerializer: JsonSerializer, jsonDeserializer: JsonDeserializer): JsonRpcServer = {
    new JsonRpcServer(
      new JsonRpcMethodRepository(),
      jsonSender,
      jsonSerializer,
      jsonDeserializer
    )
  }
}
