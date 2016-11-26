package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.communicators.{JsonReceiver, JsonSender}
import io.github.shogowada.scala.jsonrpc.models.Models
import io.github.shogowada.scala.jsonrpc.models.Models._
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

import scala.concurrent.{Future, Promise}

class JsonRpcClient
(
    jsonRpcPromisedResponseRepository: JsonRpcPromisedResponseRepository,
    jsonSender: JsonSender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer
) extends JsonReceiver {

  private type ErrorOrResult = Either[JsonRpcErrorResponse, JsonRpcResponse]

  def send(request: JsonRpcRequest): Future[ErrorOrResult] = {
    jsonSerializer.serialize(request)
        .map(json => send(request.id, json))
        .getOrElse(Future.failed(new IllegalArgumentException(s"$request could not be serialized into JSON.")))
  }

  private def send(id: Id, json: String): Future[ErrorOrResult] = {
    val promisedResponse: Promise[ErrorOrResult] = jsonRpcPromisedResponseRepository.addAndGet(id)
    jsonSender.send(json)
    promisedResponse.future
  }

  def send(notification: JsonRpcNotification): Unit = {
    jsonSerializer.serialize(notification)
        .foreach(jsonSender.send)
  }

  override def receive(json: String): Unit = {
    maybeGetResultAsRight(json)
        .orElse {
          maybeGetErrorAsLeft(json)
        }
        .foreach(receive)
  }

  private def maybeGetResultAsRight(json: String): Option[ErrorOrResult] = {
    jsonDeserializer.deserialize[JsonRpcResponse](json)
        .filter(response => response.jsonrpc == Models.jsonRpc)
        .map(response => Right(response))
  }

  private def maybeGetErrorAsLeft(json: String): Option[ErrorOrResult] = {
    jsonDeserializer.deserialize[JsonRpcErrorResponse](json)
        .filter(response => response.jsonrpc == Models.jsonRpc)
        .map(response => Left(response))
  }

  private def receive(errorOrResult: ErrorOrResult): Unit = {
    errorOrResult
        .fold(
          error => error.id,
          result => Some(result.id)
        )
        .flatMap((id: Id) => jsonRpcPromisedResponseRepository.getAndRemove(id))
        .foreach((promisedResponse: Promise[ErrorOrResult]) => promisedResponse.success(errorOrResult))
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
