package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcError, JsonRpcRequest}
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.Handler
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.blackbox


class JsonRpcServerBuilder[JSON_SERIALIZER <: JsonSerializer]
(
    val methodNameToHandlerMap: mutable.Map[String, Handler],
    val jsonSerializer: JSON_SERIALIZER
) {
  def bindApi[API](api: API): Unit = macro JsonRpcServerBuilderMacro.bindApi[JSON_SERIALIZER, API]

  def build: JsonRpcServer[JSON_SERIALIZER] = new JsonRpcServer(
    methodNameToHandlerMap.toMap,
    jsonSerializer
  )
}

object JsonRpcServerBuilder {
  def apply[JSON_SERIALIZER <: JsonSerializer](jsonSerializer: JSON_SERIALIZER): JsonRpcServerBuilder[JSON_SERIALIZER] =
    new JsonRpcServerBuilder[JSON_SERIALIZER](mutable.Map(), jsonSerializer)
}

object JsonRpcServerBuilderMacro {
  def bindApi[JSON_SERIALIZER <: JsonSerializer, API: c.WeakTypeTag]
  (c: blackbox.Context)
  (api: c.Expr[API])
  : c.Expr[Unit] = {
    import c.universe._

    val methodNameToHandlerMap = q"${c.prefix.tree}.methodNameToHandlerMap"

    val apiType: Type = weakTypeOf[API]
    val methodNameToHandlerList = MacroUtils[c.type](c).getApiMethods(apiType)
        .map((apiMember: MethodSymbol) => createMethodNameToHandler[c.type, API](c)(api, apiMember))

    c.Expr[Unit](
      q"""
          $methodNameToHandlerMap ++= Map(..$methodNameToHandlerList)
          """
    )
  }

  private def createMethodNameToHandler[CONTEXT <: blackbox.Context, API]
  (c: blackbox.Context)
  (api: c.Expr[API], method: c.universe.MethodSymbol)
  : c.Expr[(String, Handler)] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val jsonSerializer = q"${c.prefix.tree}.jsonSerializer"
    val methodName = macroUtils.getMethodName(method)

    val parameterLists = method.asMethod.paramLists

    val parameterTypes: Iterable[Type] = parameterLists
        .flatMap((paramList: List[Symbol]) => paramList)
        .map((param: Symbol) => param.typeSignature)

    val parameterType: Tree = macroUtils.getParameterType(method)

    def arguments(params: TermName): Seq[Tree] = {
      Range(0, parameterTypes.size)
          .map(index => TermName(s"_${index + 1}"))
          .map(fieldName => q"$params.$fieldName")
          .toSeq
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
            Future(None)
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

      def maybeJsonRpcRequest(json: TermName) = c.Expr[JsonRpcRequest[parameterType.type]](
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
                    ))
                    .map((response) => $jsonSerializer.serialize(response))
                })
                .getOrElse(Future($maybeInvalidParamsErrorJson))
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
}
