package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Types.{JsonRpcNotificationMethod, JsonRpcRequestMethod}
import io.github.shogowada.scala.jsonrpc.models._
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.Future
import scala.language.higherKinds

class JsonRpcRequestSingleMethodServer[PARAMS, ERROR, RESULT]
(
    methodName: String,
    method: JsonRpcRequestMethod[PARAMS, ERROR, RESULT]
) extends JsonReceiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive[SERIALIZER[_], DESERIALIZER[_]]
  (
      json: String
  )(
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]] = {
//    jsonSerializer.deserialize[JsonRpcRequest[PARAMS]](json)
//        .filter(request => request.jsonrpc == Constants.JsonRpc)
//        .filter(request => request.method == methodName)
//        .map(request => handle(request)(jsonSerializer))
//        .getOrElse(Future(None))
    Future(None)
  }

  private def handle[SERIALIZER[_], DESERIALIZER[_]]
  (
      request: JsonRpcRequest[PARAMS]
  )(
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]] = {
//    method(request).map {
//      case Right(result: JsonRpcResponse[RESULT]) => jsonSerializer.serialize(result)
//      case Left(error: JsonRpcErrorResponse[ERROR]) => jsonSerializer.serialize(error)
//    }
    Future(None)
  }
}

class JsonRpcNotificationSingleMethodServer[PARAMS]
(
    methodName: String,
    method: JsonRpcNotificationMethod[PARAMS]
) extends JsonReceiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive[SERIALIZER[_], DESERIALIZER[_]]
  (
      json: String
  )(
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]] = {
    Future(
//      jsonSerializer.deserialize[JsonRpcNotification[PARAMS]](json)
//          .filter(notification => notification.jsonrpc == Constants.JsonRpc)
//          .filter(notification => notification.method == methodName)
//          .flatMap(notification => {
//            method(notification)
//            None
//          })
      None
    )
  }
}
