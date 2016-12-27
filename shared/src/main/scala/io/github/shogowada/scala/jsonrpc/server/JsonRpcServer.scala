package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.Handler
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.concurrent.Future
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServer[JSON_SERIALIZER <: JsonSerializer]
(
    val methodNameToHandlerMap: Map[String, Handler],
    val jsonSerializer: JSON_SERIALIZER
) {
  def bindApi[API](api: API): JsonRpcServer[JSON_SERIALIZER] = macro JsonRpcServerMacro.bindApi[JSON_SERIALIZER, API]

  def receive(json: String): Future[Option[String]] = macro JsonRpcServerMacro.receive
}

object JsonRpcServer {
  type Handler = (String) => Future[Option[String]]

  def apply[JSON_SERIALIZER <: JsonSerializer](jsonSerializer: JSON_SERIALIZER): JsonRpcServer[JSON_SERIALIZER] =
    JsonRpcServer(Map(), jsonSerializer)

  def apply[JSON_SERIALIZER <: JsonSerializer]
  (
      methodNameToHandlerMap: Map[String, Handler],
      jsonSerializer: JSON_SERIALIZER
  ): JsonRpcServer[JSON_SERIALIZER] =
    new JsonRpcServer(methodNameToHandlerMap, jsonSerializer)
}

object JsonRpcServerMacro {
  def bindApi[JSON_SERIALIZER <: JsonSerializer, API: c.WeakTypeTag]
  (c: blackbox.Context)
  (api: c.Expr[API])
  : c.Expr[JsonRpcServer[JSON_SERIALIZER]] = {
    import c.universe._

    val apiType: Type = weakTypeOf[API]
    val methodNameToHandlerList = MacroUtils[c.type](c).getApiMethods(apiType)
        .map((apiMember: MethodSymbol) => createMethodNameToHandler(c)(api, apiMember))

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
    val methodName = q"""${MacroUtils[c.type](c).getMethodName(method)}"""

    val parameterTypes: Iterable[Type] = method.asMethod.paramLists
        .flatMap((paramList: List[Symbol]) => paramList)
        .map((param: Symbol) => param.typeSignature)

    val parameterType: Tree = MacroUtils[c.type](c).getParameterType(method)

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
                  .map((result) => JsonRpcResultResponse(jsonrpc = Constants.JsonRpc, id = request.id, result = result))
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

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"
    val methodNameToHandlerMap: Tree = q"${c.prefix.tree}.methodNameToHandlerMap"

    val maybeJsonRpcMethod =
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .filter(method => method.jsonrpc == Constants.JsonRpc)
          """

    val maybeHandler =
      q"""
          $maybeJsonRpcMethod
              .flatMap((jsonRpcMethod: JsonRpcMethod) => {
                $methodNameToHandlerMap.get(jsonRpcMethod.method)
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
