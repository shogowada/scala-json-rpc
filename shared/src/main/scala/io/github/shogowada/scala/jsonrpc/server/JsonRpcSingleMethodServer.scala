package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Types.{JsonRpcNotificationMethod, JsonRpcRequestMethod}
import io.github.shogowada.scala.jsonrpc.models._
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.Future

class JsonRpcRequestSingleMethodServer[PARAMS, ERROR, RESULT]
(
    jsonSerializer: JsonSerializer,
    methodName: String,
    method: JsonRpcRequestMethod[PARAMS, ERROR, RESULT]
) extends JsonReceiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive(json: String): Future[Option[String]] = {
    jsonSerializer.deserialize[JsonRpcRequest[PARAMS]](json)
        .filter(request => request.jsonrpc == Constants.JsonRpc)
        .filter(request => request.method == methodName)
        .map(handle)
        .getOrElse(Future(None))
  }

  private def handle(request: JsonRpcRequest[PARAMS]): Future[Option[String]] = {
    method(request).map {
      case Right(result: JsonRpcResponse[RESULT]) => jsonSerializer.serialize(result)
      case Left(error: JsonRpcErrorResponse[ERROR]) => jsonSerializer.serialize(error)
    }
  }
}

class JsonRpcNotificationSingleMethodServer[PARAMS]
(
    jsonSerializer: JsonSerializer,
    methodName: String,
    method: JsonRpcNotificationMethod[PARAMS]
) extends JsonReceiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive(json: String): Future[Option[String]] = {
    Future(
      jsonSerializer.deserialize[JsonRpcNotification[PARAMS]](json)
          .filter(notification => notification.jsonrpc == Constants.JsonRpc)
          .filter(notification => notification.method == methodName)
          .flatMap(notification => {
            method(notification)
            None
          })
    )
  }
}
