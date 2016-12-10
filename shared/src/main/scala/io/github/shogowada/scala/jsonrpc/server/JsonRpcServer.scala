package io.github.shogowada.scala.jsonrpc.server

import java.lang.reflect.{Method, Proxy => JavaProxy}

import io.github.shogowada.scala.jsonrpc.communicators.{JsonReceiver, JsonSender}
import io.github.shogowada.scala.jsonrpc.models.Models
import io.github.shogowada.scala.jsonrpc.models.Models._
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

import scala.reflect.{ClassTag, classTag}

class JsonRpcServer
(
    jsonSender: JsonSender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer
) extends JsonReceiver {

  var methodNameToJsonReceiverMap: Map[String, JsonReceiver] = Map()

  def bindApi[API: ClassTag](api: API): Unit = {
    val clazz: Class[_] = classTag[API].runtimeClass
    clazz.getMethods.foreach(method => bindApiMethod(api, method))
  }

  private def bindApiMethod[API](api: API, method: Method): Unit = {
    JsonRpcApiMethodBinder().bind[API](this, api, method)
  }

  def bindRequestMethod[PARAMS, ERROR, RESULT](methodName: String, method: JsonRpcRequestMethod[PARAMS, ERROR, RESULT]): Unit = {
    val server = new JsonRpcRequestSingleMethodServer[PARAMS, ERROR, RESULT](jsonSender, jsonSerializer, jsonDeserializer, methodName, method)
    bind(methodName, server)
  }

  def bindNotificationMethod[PARAMS](methodName: String, method: JsonRpcNotificationMethod[PARAMS]): Unit = {
    val server = new JsonRpcNotificationSingleMethodServer[PARAMS](jsonDeserializer, methodName, method)
    bind(methodName, server)
  }

  private def bind(methodName: String, jsonReceiver: JsonReceiver): Unit = {
    this.synchronized {
      methodNameToJsonReceiverMap = methodNameToJsonReceiverMap + (methodName -> jsonReceiver)
    }
  }

  override def receive(json: String): Unit = {
    val errorOrJsonRpcMethod: Either[JsonRpcErrorResponse[String], JsonRpcMethod] =
      jsonDeserializer.deserialize[JsonRpcMethod](json)
          .filter(method => method.jsonrpc == Models.jsonRpc)
          .toRight(JsonRpcResponse(JsonRpcErrors.invalidRequest))

    val errorOrJsonReceiver: Either[JsonRpcErrorResponse[String], JsonReceiver] =
      errorOrJsonRpcMethod.right
          .flatMap((jsonRpcMethod: JsonRpcMethod) => getErrorOrJsonReceiver(jsonRpcMethod))

    errorOrJsonReceiver.fold(
      (error: JsonRpcErrorResponse[String]) => send(error),
      (jsonReceiver: JsonReceiver) => jsonReceiver.receive(json)
    )
  }

  private def getErrorOrJsonReceiver(jsonRpcMethod: JsonRpcMethod): Either[JsonRpcErrorResponse[String], JsonReceiver] = {
    methodNameToJsonReceiverMap.get(jsonRpcMethod.method)
        .toRight(JsonRpcResponse(JsonRpcErrors.methodNotFound.copy(data = Option(s"Method with name '${jsonRpcMethod.method}' was not found."))))
  }

  private def send[T](payload: T): Unit = {
    jsonSerializer.serialize(payload)
        .foreach(jsonSender.send)
  }
}

object JsonRpcServer {
  def apply(jsonSender: JsonSender, jsonSerializer: JsonSerializer, jsonDeserializer: JsonDeserializer) =
    new JsonRpcServer(jsonSender, jsonSerializer, jsonDeserializer)
}
