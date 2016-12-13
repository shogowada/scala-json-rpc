package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.models.Types.Id
import io.github.shogowada.scala.jsonrpc.models._
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.{Future, Promise}
import scala.language.higherKinds

class JsonRpcSingleMethodClient[SERIALIZER[_], DESERIALIZER[_], PARAMS, ERROR, RESULT]
(
    jsonRpcPromisedResponseRepository: JsonRpcPromisedResponseRepository[ERROR, RESULT],
    jsonSender: JsonSender,
    jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER],
    method: String
) extends JsonReceiver {

  private type ErrorOrResult = Either[JsonRpcErrorResponse[ERROR], JsonRpcResponse[RESULT]]

  def send(request: JsonRpcRequest[PARAMS]): Future[ErrorOrResult] = {
    jsonSerializer.serialize(request)
        .map(json => send(request.id, json))
        .getOrElse(Future.failed(new IllegalArgumentException(s"$request could not be serialized into JSON.")))
  }

  private def send(id: Id, json: String): Future[ErrorOrResult] = {
    val promisedResponse: Promise[ErrorOrResult] = jsonRpcPromisedResponseRepository.addAndGet(id)
    jsonSender.send(json)
    promisedResponse.future
  }

  def send(notification: JsonRpcNotification[PARAMS]): Unit = {
    jsonSerializer.serialize(notification)
        .foreach(jsonSender.send)
  }

  override def receive(json: String): Unit = {
    maybeGetResultAsRight(json)
        .orElse {
          maybeGetErrorAsLeft(json)
        }
        .foreach(handle)
  }

  private def maybeGetResultAsRight(json: String): Option[ErrorOrResult] = {
    jsonSerializer.deserialize[JsonRpcResponse[RESULT]](json)
        .filter(response => response.jsonrpc == Constants.JsonRpc)
        .map(response => Right(response))
  }

  private def maybeGetErrorAsLeft(json: String): Option[ErrorOrResult] = {
    jsonSerializer.deserialize[JsonRpcErrorResponse[ERROR]](json)
        .filter(response => response.jsonrpc == Constants.JsonRpc)
        .map(response => Left(response))
  }

  private def handle(errorOrResult: ErrorOrResult): Unit = {
    errorOrResult
        .fold(
          error => error.id,
          result => Some(result.id)
        )
        .flatMap((id: Id) => jsonRpcPromisedResponseRepository.getAndRemove(id))
        .foreach((promisedResponse: Promise[ErrorOrResult]) => promisedResponse.success(errorOrResult))
  }
}
