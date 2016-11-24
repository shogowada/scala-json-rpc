package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.communicators.{Receiver, Sender}
import io.github.shogowada.scala.jsonrpc.models.Models._
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class JsonRpcServer
(
    jsonRpcMethodRepository: JsonRpcMethodRepository,
    jsonRpcRequestHandler: JsonRpcRequestHandler,
    jsonRpcNotificationHandler: JsonRpcNotificationHandler,
    sender: Sender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer
) extends Receiver {
  val requestHandler: (JsonRpcRequest) => Future[JsonRpcResponse] =
    jsonRpcRequestHandler.handle(_)(jsonRpcMethodRepository)
  val notificationHandler: (JsonRpcNotification) => Unit =
    jsonRpcNotificationHandler.handle(_)(jsonRpcMethodRepository)

  def bindApi[T](api: T): Unit = bindApi(() => api)

  def bindApi[T](apiFactory: () => T): Unit = {
    jsonRpcMethodRepository.bind[T](apiFactory)
  }

  override def receive(json: String): Unit = {
    val maybeRequest = jsonDeserializer.deserialize[JsonRpcRequest](json)
    if (maybeRequest.isDefined) {
      requestHandler(maybeRequest.get).onComplete {
        case Success(response) => send(response)
        case Failure(error) => send(JsonRpcResponse(JsonRpcErrors.internalError.copy(data = Some(error.toString))))
      }
      return
    }
    val maybeNotification = jsonDeserializer.deserialize[JsonRpcNotification](json)
    if (maybeNotification.isDefined) {
      notificationHandler(maybeNotification.get)
      return
    }
    send(JsonRpcResponse(JsonRpcErrors.invalidRequest))
  }

  private def send(response: JsonRpcResponse): Unit = {
    jsonSerializer.serialize(response)
        .foreach(sender.send)
  }
}
