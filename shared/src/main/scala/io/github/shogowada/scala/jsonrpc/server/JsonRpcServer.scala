package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.Handler

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServer[JSON_SERIALIZER]
(
    val methodNameToHandlerMap: Map[String, Handler],
    val jsonSerializer: JSON_SERIALIZER
) {
  def bindApi[API](api: API): JsonRpcServer[JSON_SERIALIZER] = macro JsonRpcServerMacro.bindApi[JSON_SERIALIZER, API]

  def receive(json: String): Future[Option[String]] = macro JsonRpcServerMacro.receive
}

object JsonRpcServer {
  type Handler = (String) => Future[Option[String]]

  def apply[JSON_SERIALIZER](jsonSerializer: JSON_SERIALIZER): JsonRpcServer[JSON_SERIALIZER] =
    JsonRpcServer(Map(), jsonSerializer)

  def apply[JSON_SERIALIZER]
  (
      methodNameToHandlerMap: Map[String, Handler],
      jsonSerializer: JSON_SERIALIZER
  ): JsonRpcServer[JSON_SERIALIZER] =
    new JsonRpcServer(methodNameToHandlerMap, jsonSerializer)
}

object JsonRpcServerMacro {
  def bindApi[JSON_SERIALIZER, API: c.WeakTypeTag]
  (c: blackbox.Context)
  (api: c.Expr[API])
  : c.Expr[JsonRpcServer[JSON_SERIALIZER]] = {
    import c.universe._

    val apiType: Type = weakTypeOf[API]
    val apiMembers: MemberScope = apiType.decls

    val methodNameToHandlerList = apiMembers
        .filter((apiMember: Symbol) => isJsonRpcMethod(c)(apiMember))
        .map((apiMember: Symbol) => createMethodNameToHandler(c)(api, apiMember.asMethod))

    c.Expr[JsonRpcServer[JSON_SERIALIZER]](
      q"""
          JsonRpcServer(
            ${c.prefix.tree}.methodNameToHandlerMap ++ Map(..$methodNameToHandlerList),
            ${c.prefix.tree}.jsonSerializer
          )
          """
    )
  }

  private def isJsonRpcMethod(c: blackbox.Context)(method: c.universe.Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  private def createMethodNameToHandler[API]
  (c: blackbox.Context)
  (api: c.Expr[API], method: c.universe.MethodSymbol)
  : c.Expr[(String, Handler)] = {
    import c.universe._

    val jsonSerializer = q"${c.prefix.tree}.jsonSerializer"
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

    val params = TermName("params")

    val handler = c.Expr[Handler](
      q"""
          (json: String) => {
            import io.github.shogowada.scala.jsonrpc.Constants
            import io.github.shogowada.scala.jsonrpc.Models._
            $jsonSerializer.deserialize[JsonRpcRequest[$parameterType]](json)
              .map(request => {
                val $params = request.params
                $api.$method(..${arguments(params)})
                  .map((result) => JsonRpcResponse(jsonrpc = Constants.JsonRpc, id = request.id, result = result))
                  .map((response) => $jsonSerializer.serialize(response))
              })
              .getOrElse(Future(None))
          }
          """
    )

    c.Expr[(String, Handler)](q"""$methodName -> $handler""")
  }

  def receive
  (c: blackbox.Context)
  (json: c.Expr[String])
  : c.Expr[Future[Option[String]]] = {
    import c.universe._

    val maybeJsonRpcMethod =
      q"""
          ${c.prefix.tree}.jsonSerializer.deserialize[JsonRpcMethod]($json)
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
