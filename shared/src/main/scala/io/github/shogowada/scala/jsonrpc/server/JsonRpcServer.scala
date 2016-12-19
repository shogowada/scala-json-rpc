package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros.blackbox

class JsonRpcServer() {

  var methodNameToHandlerMap: Map[String, (String) => Future[Option[String]]] = Map()

  def bindApi[SERIALIZER[_], DESERIALIZER[_], API](api: API, jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]): Unit = macro JsonRpcServerImpl.bindApi[SERIALIZER, DESERIALIZER, API]

  def receive[SERIALIZER[_], DESERIALIZER[_]]
  (
      json: String,
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]] = macro JsonRpcServerImpl.receive[SERIALIZER, DESERIALIZER]

  def deserialize[SERIALIZER[_], DESERIALIZER[_], T](json: String, jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]): Option[T] = macro JsonRpcServerImpl.deserialize[SERIALIZER, DESERIALIZER, T]
}

object JsonRpcServer {
  def apply() = new JsonRpcServer()
}

object JsonRpcServerImpl {
  def deserialize[SERIALIZER[_], DESERIALIZER[_], T: c.WeakTypeTag]
  (c: blackbox.Context)
  (json: c.Expr[String], jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]]): c.Expr[Option[T]] = {
    import c.universe._
    val jsonType = weakTypeOf[T]
    c.Expr[Option[T]](q"$jsonSerializer.deserialize[$jsonType]($json)")
  }

  def bindApi[SERIALIZER[_], DESERIALIZER[_], API: c.WeakTypeTag]
  (c: blackbox.Context)
  (api: c.Expr[API], jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[Unit] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    val apiMembers: Iterable[Symbol] = apiType.decls
    val apiMemberStatements: Iterable[Tree] = apiMembers
        .filter((apiMember: Symbol) => isJsonRpcMethod(c)(apiMember))
        .map((method: Symbol) => {
          q"${c.prefix.tree}.methodNameToHandlerMap = ${c.prefix.tree}.methodNameToHandlerMap + (${createHandler(c)(method)})"
        })
    c.Expr[Unit](q"{ ..$apiMemberStatements }")
  }

  private def isJsonRpcMethod(c: blackbox.Context)(method: c.universe.Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  private def createHandler(c: blackbox.Context)(method: c.universe.Symbol): c.Expr[(String) => Future[Option[String]]] = {
    import c.universe._
    c.Expr(q"""${method.fullName} -> ((json:String) => Future(None))""")
  }

  def receive[SERIALIZER[_], DESERIALIZER[_]]
  (c: blackbox.Context)
  (json: c.Expr[String], jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[Future[Option[String]]] = {
    /*

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
    * */
    import c.universe._
    val errorOrJsonRpcMethod =
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .filter(method => method.jsonrpc == Constants.JsonRpc)
              .toRight(JsonRpcResponse(JsonRpcErrors.invalidRequest))
          """
    val errorOrJsonReceiver =
      q"""
          $errorOrJsonRpcMethod.right
              .flatMap((jsonRpcMethod: JsonRpcMethod) => getErrorOrJsonReceiver(jsonRpcMethod))
          """
    /*
      def getErrorOrJsonReceiver(jsonRpcMethod: JsonRpcMethod): Either[JsonRpcErrorResponse[String], JsonReceiver] = {
    methodNameToJsonReceiverMap.get(jsonRpcMethod.method)
        .toRight(JsonRpcResponse(JsonRpcErrors.methodNotFound.copy(data = Option(s"Method with name '${jsonRpcMethod.method}' was not found."))))
  }
    * */
    c.Expr(
      q"""
          import io.github.shogowada.scala.jsonrpc.models._
          {$errorOrJsonReceiver Future(None)}
          """
    )
  }
}
