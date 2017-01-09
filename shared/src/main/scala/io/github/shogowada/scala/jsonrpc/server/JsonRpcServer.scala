package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcError, JsonRpcRequest}
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.Handler
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServer[JSON_SERIALIZER <: JsonSerializer]
(
    val jsonSerializer: JSON_SERIALIZER,
    val executionContext: ExecutionContext
) {
  val lock = new Object()

  var methodNameToHandlerMap: Map[String, Handler] = Map()

  def bindApi[API](api: API): Unit = macro JsonRpcServerMacro.bindApi[API]

  def receive(json: String): Future[Option[String]] = macro JsonRpcServerMacro.receive
}

object JsonRpcServer {
  type Handler = (String) => Future[Option[String]]

  def apply[JSON_SERIALIZER <: JsonSerializer](jsonSerializer: JSON_SERIALIZER)(implicit executionContext: ExecutionContext) = {
    new JsonRpcServer(jsonSerializer, executionContext)
  }
}

object JsonRpcServerMacro {
  def bindApi[API: c.WeakTypeTag](c: blackbox.Context)(api: c.Expr[API]): c.Expr[Unit] = {
    import c.universe._

    val lock = q"${c.prefix.tree}.lock"
    val methodNameToHandlerMap = q"${c.prefix.tree}.methodNameToHandlerMap"

    val apiType: Type = weakTypeOf[API]
    val methodNameToHandlerList = MacroUtils[c.type](c).getApiMethods(apiType)
        .map((apiMember: MethodSymbol) => createMethodNameToHandler[c.type, API](c)(api, apiMember))

    c.Expr[Unit](
      q"""
          $lock.synchronized($methodNameToHandlerMap = $methodNameToHandlerMap ++ Map(..$methodNameToHandlerList))
          """
    )
  }

  private def createMethodNameToHandler[CONTEXT <: blackbox.Context, API](c: blackbox.Context)(
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[(String, Handler)] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val jsonSerializer = q"${c.prefix.tree}.jsonSerializer"
    val executionContext = q"${c.prefix.tree}.executionContext"
    val methodName = macroUtils.getMethodName(method)

    val parameterLists: List[List[Symbol]] = method.asMethod.paramLists

    val parameterTypes: Iterable[Type] = parameterLists
        .flatten
        .map((param: Symbol) => param.typeSignature)

    val parameterType: Tree = macroUtils.getParameterType(method)

    def arguments(params: TermName): Seq[Tree] = {
      Range(0, parameterTypes.size)
          .map(index => TermName(s"_${index + 1}"))
          .map(fieldName => q"$params.$fieldName")
    }

    val json = TermName("json")
    val params = TermName("params")

    def methodInvocation(params: TermName) = {
      if (parameterLists.isEmpty) {
        q"$api.$method"
      } else {
        q"$api.$method(..${arguments(params)})"
      }
    }

    def notificationHandler = c.Expr[Handler](
      q"""
          ($json: String) => {
            ..${macroUtils.imports}
            $jsonSerializer.deserialize[JsonRpcNotification[$parameterType]]($json)
              .foreach(notification => {
                val $params = notification.params
                ${methodInvocation(params)}
              })
            Future(None)($executionContext)
          }
          """
    )

    def requestHandler = {
      val request = TermName("request")

      val maybeInvalidParamsErrorJson: c.Expr[Option[String]] =
        macroUtils.createMaybeErrorJson(
          c.Expr[String](q"$json"),
          c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidParams")
        )

      def maybeJsonRpcRequest(json: TermName) = c.Expr[Option[JsonRpcRequest[parameterType.type]]](
        q"""$jsonSerializer.deserialize[JsonRpcRequest[$parameterType]]($json)"""
      )

      c.Expr[Handler](
        q"""
            ($json: String) => {
              ..${macroUtils.imports}
              ${maybeJsonRpcRequest(json)}
                .map(($request: JsonRpcRequest[$parameterType]) => {
                  val $params = $request.params
                  ${methodInvocation(params)}
                    .map((result) => JsonRpcResultResponse(
                      jsonrpc = Constants.JsonRpc,
                      id = $request.id,
                      result = result
                    ))($executionContext)
                    .map((response) => $jsonSerializer.serialize(response))($executionContext)
                })
                .getOrElse(Future($maybeInvalidParamsErrorJson)($executionContext))
            }
            """
      )
    }

    def handler: c.Expr[Handler] = if (macroUtils.isNotificationMethod(method)) {
      notificationHandler
    } else {
      requestHandler
    }

    c.Expr[(String, Handler)](q"""$methodName -> $handler""")
  }

  def receive(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Future[Option[String]]] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"
    val methodNameToHandlerMap: Tree = q"${c.prefix.tree}.methodNameToHandlerMap"
    val executionContext: Tree = q"${c.prefix.tree}.executionContext"

    val maybeParseErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.parseError"))
    val maybeInvalidRequestErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidRequest"))
    val maybeMethodNotFoundErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.methodNotFound"))

    val maybeErrorJsonOrMethodName = c.Expr[Either[Option[String], String]](
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .toRight($maybeParseErrorJson)
              .right.flatMap(method => {
                if(method.jsonrpc != Constants.JsonRpc) {
                  Left($maybeInvalidRequestErrorJson)
                } else {
                  Right(method.method)
                }
              })
          """
    )

    val maybeErrorJsonOrHandler = c.Expr[Either[Option[String], Handler]](
      q"""
          $maybeErrorJsonOrMethodName
              .right.flatMap((methodName: String) => {
                $methodNameToHandlerMap.get(methodName)
                  .toRight($maybeMethodNotFoundErrorJson)
              })
          """
    )

    val futureMaybeJson = c.Expr[Future[Option[String]]](
      q"""
          $maybeErrorJsonOrHandler.fold[Future[Option[String]]](
            maybeErrorJson => Future(maybeErrorJson)($executionContext),
            handler => handler($json)
          )
          """
    )

    c.Expr(
      q"""
          ..${macroUtils.imports}
          $futureMaybeJson
          """
    )
  }
}
