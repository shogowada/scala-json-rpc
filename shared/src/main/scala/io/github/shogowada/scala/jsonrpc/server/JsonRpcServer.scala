package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.communicators.{JsonReceiver, JsonSender}
import io.github.shogowada.scala.jsonrpc.models.Models._
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

import scala.util.{Failure, Success}

class JsonRpcServer
(
    jsonRpcMethodRepository: JsonRpcMethodRepository,
    jsonRpcRequestHandler: JsonRpcRequestHandler,
    jsonRpcNotificationHandler: JsonRpcNotificationHandler,
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
    val requestHandled = maybeHandleRequest(json)
    if (requestHandled) {
      return
    }
    val notificationHandled = maybeHandleNotification(json)
    if (notificationHandled) {
      return
    }
    send(JsonRpcResponse(JsonRpcErrors.invalidRequest))
  }

  private def maybeHandleRequest(json: String): Boolean = {
    val maybeRequest = jsonDeserializer.deserialize[JsonRpcRequest](json)
    if (maybeRequest.isEmpty) {
      false
    } else {
      val request = maybeRequest.get
      val maybeMethod = jsonRpcMethodRepository.getRequestMethod(request.method)
      if (maybeMethod.isDefined) {
        val method = maybeMethod.get
        jsonRpcRequestHandler.handle(request, method).onComplete {
          case Success(response) => send(response)
          case Failure(error) => send(JsonRpcResponse(request.id, JsonRpcErrors.internalError.copy(data = Some(error.toString))))
        }
      } else {
        sendMethodNotFound(request.id, request.method)
      }
      true
    }
  }

  private def maybeHandleNotification(json: String): Boolean = {
    val maybeNotification = jsonDeserializer.deserialize[JsonRpcNotification](json)
    if (maybeNotification.isEmpty) {
      false
    } else {
      val notification = maybeNotification.get
      val maybeMethod = jsonRpcMethodRepository.getNotificationMethod(notification.method)
      if (maybeMethod.isDefined) {
        val method = maybeMethod.get
        jsonRpcNotificationHandler.handle(notification, method)
      } else {
        sendMethodNotFound(notification.method)
      }
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

  private def send(response: JsonRpcResponse): Unit = {
    jsonSerializer.serialize(response)
        .foreach(jsonSender.send)
  }
}

object JsonRpcServer {
  def apply(jsonSender: JsonSender, jsonSerializer: JsonSerializer, jsonDeserializer: JsonDeserializer): JsonRpcServer = {
    new JsonRpcServer(
      new JsonRpcMethodRepository(),
      new JsonRpcRequestHandler(),
      new JsonRpcNotificationHandler(),
      jsonSender,
      jsonSerializer,
      jsonDeserializer
    )
  }
}
