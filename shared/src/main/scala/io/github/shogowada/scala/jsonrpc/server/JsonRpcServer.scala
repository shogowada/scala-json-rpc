package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.Handler

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros.blackbox

class JsonRpcServer(val methodNameToHandlerMap: Map[String, Handler]) {
  def bindApi[SERIALIZER[_], DESERIALIZER[_], API]
  (
      api: API,
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): JsonRpcServer = macro JsonRpcServerImpl.bindApi[SERIALIZER, DESERIALIZER, API]

  def receive[SERIALIZER[_], DESERIALIZER[_]]
  (
      json: String,
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]] = macro JsonRpcServerImpl.receive[SERIALIZER, DESERIALIZER]
}

object JsonRpcServer {
  type Handler = (String) => Future[Option[String]]

  def apply(): JsonRpcServer = JsonRpcServer(Map())

  def apply(methodNameToHandlerMap: Map[String, Handler]): JsonRpcServer = new JsonRpcServer(methodNameToHandlerMap)
}

object JsonRpcServerImpl {
  def bindApi[SERIALIZER[_], DESERIALIZER[_], API: c.WeakTypeTag]
  (c: blackbox.Context)
  (api: c.Expr[API], jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[JsonRpcServer] = {
    import c.universe._

    val apiType: Type = weakTypeOf[API]
    val apiMembers: MemberScope = apiType.decls

    val methodNameToHandlerList = apiMembers
        .filter((apiMember: Symbol) => isJsonRpcMethod(c)(apiMember))
        .map((apiMember: Symbol) => createMethodNameToHandler(c)(api, apiMember.asMethod, jsonSerializer))

    c.Expr[JsonRpcServer](q"""JsonRpcServer(${c.prefix.tree}.methodNameToHandlerMap ++ Map(..$methodNameToHandlerList))""")
  }

  private def isJsonRpcMethod(c: blackbox.Context)(method: c.universe.Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  private def createMethodNameToHandler[SERIALIZER[_], DESERIALIZER[_], API]
  (c: blackbox.Context)
  (api: c.Expr[API], method: c.universe.MethodSymbol, jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[(String, Handler)] = {
    import c.universe._

    val methodName = q"""${method.fullName}"""

    val parameterTypes: Iterable[Type] = method.asMethod.paramLists
        .flatMap((paramList: List[Symbol]) => paramList)
        .map((param: Symbol) => param.typeSignature)

    val parameterType: Tree = tq"(..$parameterTypes)"

    def arguments(params: TermName): Seq[Tree] = {
      Range(0, parameterTypes.size)
          .map(index => TermName(s"_${index + 1}"))
          .map(fieldName => q"$params.$fieldName")
          .toSeq
    }

    def methodInvocation(params: TermName): Tree = {
      q"""$api.$method(..${arguments(params)})"""
    }

    val handler =
      q"""
          (json: String) => {
            import io.github.shogowada.scala.jsonrpc.Constants
            import io.github.shogowada.scala.jsonrpc.Models._
            $jsonSerializer.deserialize[JsonRpcRequest[$parameterType]](json)
              .map(request => {
                val params = request.params
                ${methodInvocation(params = TermName("params"))}
                  .map(result => JsonRpcResponse(jsonrpc = Constants.JsonRpc, id = request.id, result = result))
                  .map(response => $jsonSerializer.serialize(response))
              })
              .getOrElse(Future(None))
          }
          """

    c.Expr[(String, Handler)](q"""$methodName -> $handler""")
  }

  def receive[SERIALIZER[_], DESERIALIZER[_]]
  (c: blackbox.Context)
  (json: c.Expr[String], jsonSerializer: c.Expr[JsonSerializer[SERIALIZER, DESERIALIZER]])
  : c.Expr[Future[Option[String]]] = {
    import c.universe._
    val maybeJsonRpcMethod =
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .filter(method => method.jsonrpc == Constants.JsonRpc)
          """

    val maybeHandler =
      q"""
          $maybeJsonRpcMethod
              .flatMap((jsonRpcMethod: JsonRpcMethod) => {
                ${c.prefix.tree}.methodNameToHandlerMap.get(jsonRpcMethod.method)
              })
          """

    val futureMaybeJson =
      q"""
          $maybeHandler.map(handler => handler($json))
            .getOrElse(Future(None))
          """

    c.Expr(
      q"""
          import io.github.shogowada.scala.jsonrpc.Constants
          import io.github.shogowada.scala.jsonrpc.Models._
          import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer._
          $futureMaybeJson
          """
    )
  }
}
