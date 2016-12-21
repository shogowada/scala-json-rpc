package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros.blackbox

class JsonRpcServer() {

  type Handler = (String) => Future[Option[String]]

  var methodNameToHandlerMap: Map[String, Handler] = Map()

  def bindApi[SERIALIZER[_], DESERIALIZER[_], API]
  (
      api: API,
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Unit = macro JsonRpcServerImpl.bindApi[SERIALIZER, DESERIALIZER, API]

  def receive[SERIALIZER[_], DESERIALIZER[_]]
  (
      json: String,
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]] = macro JsonRpcServerImpl.receive[SERIALIZER, DESERIALIZER]
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
    val apiMembers: MemberScope = apiType.decls
    val apiMemberStatements: Iterable[Tree] = apiMembers
        .filter((apiMember: Symbol) => isJsonRpcMethod(c)(apiMember))
        .map((apiMember: Symbol) => {
          val methodNameToHandler = createMethodNameToHandler(c)(api, apiMember.asMethod, jsonSerializer)
          q"${c.prefix.tree}.methodNameToHandlerMap = ${c.prefix.tree}.methodNameToHandlerMap + ($methodNameToHandler)"
        })
    c.Expr[Unit](q"{ ..$apiMemberStatements }")
  }

  private def isJsonRpcMethod(c: blackbox.Context)(method: c.universe.Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  private def createMethodNameToHandler[SERIALIZER[_], DESERIALIZER[_], API]
  (c: blackbox.Context)
  (api: c.Expr[API], method: c.universe.MethodSymbol, jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[(String) => Future[Option[String]]] = {
    import c.universe._

    val methodName = q"""${method.fullName}"""

    val parameterTypes: Iterable[Type] = method.asMethod.paramLists
        .flatMap((paramList: List[Symbol]) => paramList)
        .map((param: Symbol) => param.typeSignature)

    val parameterType: Tree = tq"(..$parameterTypes)"
    def arguments(params: TermName): Seq[Tree] = Range(0, parameterTypes.size)
        .map(index => q"$params.${TermName(s"_${index + 1}")}")
        .toSeq
    def methodInvocation(params: TermName) = q"""$api.$method(..${arguments(params)})"""

    val handler =
      q"""
          (json: String) => {
            $jsonSerializer.deserialize[JsonRpcRequest[$parameterType]](json)
              .map(request => {
                val params = request.params
                ${methodInvocation(params = TermName("params"))}
                  .map(result => $jsonSerializer.serialize(result))
              })
              .getOrElse(Future(None))
          }
          """

    c.Expr(q"""$methodName -> $handler""")
  }

  def receive[SERIALIZER[_], DESERIALIZER[_]]
  (c: blackbox.Context)
  (json: c.Expr[String], jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[Future[Option[String]]] = {
    import c.universe._
    val errorOrJsonRpcMethod =
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .filter(method => method.jsonrpc == Constants.JsonRpc)
              .toRight(JsonRpcResponse(JsonRpcErrors.invalidRequest))
          """

    val errorOrHandler =
      q"""
          $errorOrJsonRpcMethod.right
              .flatMap((jsonRpcMethod: JsonRpcMethod) => {
                ${c.prefix.tree}.methodNameToHandlerMap.get(jsonRpcMethod.method)
                  .toRight(JsonRpcResponse(JsonRpcErrors.methodNotFound.copy(data = Option(s"Method with name ' + jsonRpcMethod.method + ' was not found."))))
              })
          """

    val futureMaybeJson =
      q"""
          $errorOrHandler.fold(
            (error: JsonRpcErrorResponse[String]) => Future($jsonSerializer.serialize(error)),
            (handler: Handler) => handler($json)
          )
          """

    c.Expr(
      q"""
          import io.github.shogowada.scala.jsonrpc.Constants
          import io.github.shogowada.scala.jsonrpc.Models._
          import ${c.prefix.tree}._
          $futureMaybeJson
          """
    )
  }
}
