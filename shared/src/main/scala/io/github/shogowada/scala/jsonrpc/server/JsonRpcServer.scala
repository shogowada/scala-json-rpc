package io.github.shogowada.scala.jsonrpc.server

import java.lang.reflect.Method

import io.github.shogowada.scala.jsonrpc.models.Types.{JsonRpcNotificationMethod, JsonRpcRequestMethod}
import io.github.shogowada.scala.jsonrpc.models._
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}

class JsonRpcServer
(
    jsonSerializer: JsonSerializer
) extends JsonReceiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  var methodNameToJsonReceiverMap: Map[String, JsonReceiver] = Map()

  def bindApi[API: ClassTag](api: API): Unit = {
    val clazz: Class[_] = classTag[API].runtimeClass
    clazz.getMethods.foreach(method => bindApiMethod(api, method))
  }

  private def bindApiMethod[API](api: API, method: Method): Unit = {
    JsonRpcApiMethodBinder().bind[API](this, api, method)
  }

  def bindRequestMethod[PARAMS, ERROR, RESULT](methodName: String, method: JsonRpcRequestMethod[PARAMS, ERROR, RESULT]): Unit = {
    val server = new JsonRpcRequestSingleMethodServer[PARAMS, ERROR, RESULT](jsonSerializer, methodName, method)
    bind(methodName, server)
  }

  def bindNotificationMethod[PARAMS](methodName: String, method: JsonRpcNotificationMethod[PARAMS]): Unit = {
    val server = new JsonRpcNotificationSingleMethodServer[PARAMS](jsonSerializer, methodName, method)
    bind(methodName, server)
  }

  private def bind(methodName: String, jsonReceiver: JsonReceiver): Unit = {
    this.synchronized {
      methodNameToJsonReceiverMap = methodNameToJsonReceiverMap + (methodName -> jsonReceiver)
    }
  }

  override def receive(json: String): Future[Option[String]] = {
    val errorOrJsonRpcMethod: Either[JsonRpcErrorResponse[String], JsonRpcMethod] =
      jsonSerializer.deserialize[JsonRpcMethod](json)
          .filter(method => method.jsonrpc == Constants.JsonRpc)
          .toRight(JsonRpcResponse(JsonRpcErrors.invalidRequest))

    val errorOrJsonReceiver: Either[JsonRpcErrorResponse[String], JsonReceiver] =
      errorOrJsonRpcMethod.right
          .flatMap((jsonRpcMethod: JsonRpcMethod) => getErrorOrJsonReceiver(jsonRpcMethod))

    val maybeJsonFuture: Future[Option[String]] = errorOrJsonReceiver.fold(
      (error: JsonRpcErrorResponse[String]) => Future(jsonSerializer.serialize(error)),
      (jsonReceiver: JsonReceiver) => jsonReceiver.receive(json)
    )

    maybeJsonFuture
  }

  private def getErrorOrJsonReceiver(jsonRpcMethod: JsonRpcMethod): Either[JsonRpcErrorResponse[String], JsonReceiver] = {
    methodNameToJsonReceiverMap.get(jsonRpcMethod.method)
        .toRight(JsonRpcResponse(JsonRpcErrors.methodNotFound.copy(data = Option(s"Method with name '${jsonRpcMethod.method}' was not found."))))
  }
}

object JsonRpcServer {
  def apply(jsonSerializer: JsonSerializer) =
    new JsonRpcServer(jsonSerializer)
}
