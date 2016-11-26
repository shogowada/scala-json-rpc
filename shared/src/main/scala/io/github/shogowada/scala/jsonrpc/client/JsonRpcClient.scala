package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.communicators.{JsonReceiver, JsonSender}
import io.github.shogowada.scala.jsonrpc.models.Models
import io.github.shogowada.scala.jsonrpc.models.Models.{Id, JsonRpcNotification, JsonRpcRequest, JsonRpcResponse}
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

import scala.concurrent.{Future, Promise}

class JsonRpcClient
(
    jsonRpcPromisedResponseRepository: JsonRpcPromisedResponseRepository,
    jsonSender: JsonSender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer
) extends JsonReceiver {

  def send(request: JsonRpcRequest): Future[JsonRpcResponse] = {
    jsonSerializer.serialize(request)
        .map(json => send(request.id, json))
        .getOrElse(Future.failed(new IllegalArgumentException(s"$request could not be serialized into JSON.")))
  }

  private def send(id: Id, json: String): Future[JsonRpcResponse] = {
    val promisedResponse: Promise[JsonRpcResponse] = jsonRpcPromisedResponseRepository.addAndGet(id)
    jsonSender.send(json)
    promisedResponse.future
  }

  def send(notification: JsonRpcNotification): Unit = {
    jsonSerializer.serialize(notification)
        .foreach(jsonSender.send)
  }

  override def receive(json: String): Unit = {
    jsonDeserializer.deserialize[JsonRpcResponse](json)
        .filter(response => response.jsonrpc == Models.jsonRpc)
        .foreach(receive)
  }

  private def receive(response: JsonRpcResponse): Unit = {
    response.id
        .flatMap((id: Id) => jsonRpcPromisedResponseRepository.getAndRemove(id))
        .foreach((promisedResponse: Promise[JsonRpcResponse]) => promisedResponse.success(response))
  }
}

object JsonRpcClient {
  def apply(jsonSender: JsonSender, jsonSerializer: JsonSerializer, jsonDeserializer: JsonDeserializer) = {
    new JsonRpcClient(
      new JsonRpcPromisedResponseRepository(),
      jsonSender,
      jsonSerializer,
      jsonDeserializer
    )
  }
}
