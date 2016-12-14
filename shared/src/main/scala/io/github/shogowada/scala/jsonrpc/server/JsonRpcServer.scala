package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Types.{JsonRpcNotificationMethod, JsonRpcRequestMethod}
import io.github.shogowada.scala.jsonrpc.models._
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros.blackbox

class JsonRpcServer() extends JsonReceiver {

  import scala.concurrent.ExecutionContext.Implicits.global

  var methodNameToJsonReceiverMap: Map[String, JsonReceiver] = Map()

  def bindApi[SERIALIZER[_], DESERIALIZER[_], API](api: API, jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]): Unit = macro JsonRpcServerImpl.bindApi[SERIALIZER, DESERIALIZER, API]

  def bindRequestMethod[PARAMS, ERROR, RESULT](methodName: String, method: JsonRpcRequestMethod[PARAMS, ERROR, RESULT]): Unit = {
    val server = new JsonRpcRequestSingleMethodServer[PARAMS, ERROR, RESULT](methodName, method)
    bind(methodName, server)
  }

  def bindNotificationMethod[PARAMS](methodName: String, method: JsonRpcNotificationMethod[PARAMS]): Unit = {
    val server = new JsonRpcNotificationSingleMethodServer[PARAMS](methodName, method)
    bind(methodName, server)
  }

  private def bind(methodName: String, jsonReceiver: JsonReceiver): Unit = {
    this.synchronized {
      methodNameToJsonReceiverMap = methodNameToJsonReceiverMap + (methodName -> jsonReceiver)
    }
  }

  override def receive[SERIALIZER[_], DESERIALIZER[_]]
  (
      json: String
  )(
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]] = {
    val errorOrJsonRpcMethod: Either[JsonRpcErrorResponse[String], JsonRpcMethod] =
      jsonSerializer.deserialize[JsonRpcMethod](json)
          .filter(method => method.jsonrpc == Constants.JsonRpc)
          .toRight(JsonRpcResponse(JsonRpcErrors.invalidRequest))

    val errorOrJsonReceiver: Either[JsonRpcErrorResponse[String], JsonReceiver] =
      errorOrJsonRpcMethod.right
          .flatMap((jsonRpcMethod: JsonRpcMethod) => getErrorOrJsonReceiver(jsonRpcMethod))

    val maybeJsonFuture: Future[Option[String]] = errorOrJsonReceiver.fold(
      (error: JsonRpcErrorResponse[String]) => Future(jsonSerializer.serialize(error)),
      (jsonReceiver: JsonReceiver) => jsonReceiver.receive(json)(jsonSerializer)
    )

    maybeJsonFuture
  }

  private def getErrorOrJsonReceiver(jsonRpcMethod: JsonRpcMethod): Either[JsonRpcErrorResponse[String], JsonReceiver] = {
    methodNameToJsonReceiverMap.get(jsonRpcMethod.method)
        .toRight(JsonRpcResponse(JsonRpcErrors.methodNotFound.copy(data = Option(s"Method with name '${jsonRpcMethod.method}' was not found."))))
  }
}

object JsonRpcServer {
  def apply() = new JsonRpcServer()
}

object JsonRpcServerImpl {
  def bindApi[SERIALIZER[_], DESERIALIZER[_], API: c.WeakTypeTag]
  (c: blackbox.Context)
  (api: c.Expr[API], jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[Unit] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    val apiMembers: MemberScope = apiType.members
    apiMembers.foreach(apiMember => {
      val methodName = s"${apiType.typeSymbol.fullName}.${apiMember.fullName}"
      
    })
    c.Expr[Unit](q"")
  }
}
