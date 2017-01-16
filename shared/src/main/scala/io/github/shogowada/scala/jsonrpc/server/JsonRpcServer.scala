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
    bindApiImpl[c.type, API](c)(c.prefix.tree, None, api)
  }

  def bindApiImpl[CONTEXT <: blackbox.Context, API: c.WeakTypeTag](c: CONTEXT)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API]
  ): c.Expr[Unit] = {
    import c.universe._

    val lock = q"$server.lock"
    val methodNameToHandlerMap = q"$server.methodNameToHandlerMap"

    val apiType: Type = weakTypeOf[API]
    val methodNameToHandlerList = MacroUtils[c.type](c).getJsonRpcApiMethods(apiType)
        .map((apiMember: MethodSymbol) => createMethodNameToHandler[c.type, API](c)(server, maybeClient, api, apiMember))

    c.Expr[Unit](
      q"""
          $lock.synchronized($methodNameToHandlerMap = $methodNameToHandlerMap ++ Map(..$methodNameToHandlerList))
          """
    )
  }

  private def addHandler[CONTEXT <: blackbox.Context](c: CONTEXT)(
      methodName: c.Tree,
      handler: c.Tree
  ): c.Tree = {
    
  }

  private def createMethodNameToHandler[CONTEXT <: blackbox.Context, API](c: blackbox.Context)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[(String, Handler)] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val jsonSerializer = macroUtils.getJsonSerializer(server)
    val executionContext = macroUtils.getExecutionContext(server)
    val methodName = macroUtils.getJsonRpcMethodName(method)

    val parameterLists: List[List[Symbol]] = method.asMethod.paramLists

    val parameterTypes: Seq[Type] = parameterLists
        .flatten
        .map((param: Symbol) => param.typeSignature)

    val jsonRpcParameterType: Tree = macroUtils.getJsonRpcParameterType(method)

    def arguments(params: TermName): Seq[Tree] = {
      parameterTypes.indices
          .map(index => {
            val parameterType = parameterTypes(index)
            val fieldName = TermName(s"_${index + 1}")
            val argument = q"$params.$fieldName"
            if (macroUtils.isJsonRpcFunctionType(parameterType)) {
              maybeClient
                  .map(client => getOrCreateJsonRpcFunction[c.type](c)(server, client, parameterType, argument))
                  .getOrElse(throw new UnsupportedOperationException("To use JsonRpcFunction, you need to bind the API to JsonRpcServerAndClient."))
            } else {
              argument
            }
          })
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
            $jsonSerializer.deserialize[JsonRpcNotification[$jsonRpcParameterType]]($json)
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
          server,
          c.Expr[String](q"$json"),
          c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidParams")
        )

      def maybeJsonRpcRequest(json: TermName) = c.Expr[Option[JsonRpcRequest[jsonRpcParameterType.type]]](
        q"""$jsonSerializer.deserialize[JsonRpcRequest[$jsonRpcParameterType]]($json)"""
      )

      c.Expr[Handler](
        q"""
            ($json: String) => {
              ..${macroUtils.imports}
              ${maybeJsonRpcRequest(json)}
                .map(($request: JsonRpcRequest[$jsonRpcParameterType]) => {
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

    def handler: c.Expr[Handler] = if (macroUtils.isJsonRpcNotificationMethod(method)) {
      notificationHandler
    } else {
      requestHandler
    }

    c.Expr[(String, Handler)](q"""$methodName -> $handler""")
  }

  def getOrCreateJsonRpcFunction[CONTEXT <: blackbox.Context](c: CONTEXT)(
      server: c.Tree,
      client: c.Tree,
      jsonRpcFunctionType: c.Type,
      jsonRpcFunctionMethodName: c.Tree
  ): c.Tree = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val methodNameToHandlerMap: Tree = macroUtils.getMethodNameToHandlerMap(server)

    val newJsonRpcFunction = createJsonRpcFunction[c.type](c)(client, jsonRpcFunctionType, jsonRpcFunctionMethodName)

    q"""
        $methodNameToHandlerMap.get($jsonRpcFunctionMethodName)
          .map(jsonRpcFunction => jsonRpcFunction.asInstanceOf[$jsonRpcFunctionType])
          .getOrElse {
            val jsonRpcFunction = $newJsonRpcFunction
            $methodNameToHandlerMap.add($jsonRpcFunctionMethodName, jsonRpcFunction)
            jsonRpcFunction
          }
        """
  }

  def createJsonRpcFunction[CONTEXT <: blackbox.Context](c: CONTEXT)(
      client: c.Tree,
      jsonRpcFunctionType: c.Type,
      jsonRpcFunctionMethodName: c.Tree
  ): c.Tree = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val functionType: Type = macroUtils.getFunctionTypeOfJsonRpcFunctionType(jsonRpcFunctionType)
    val functionTypeTypeArgs: Seq[Type] = functionType.typeArgs
    val paramTypes: Seq[Type] = functionTypeTypeArgs.init
    val returnType: Type = functionTypeTypeArgs.last
    val function = macroUtils.createClientMethodAsFunction(client, jsonRpcFunctionMethodName, paramTypes, returnType)

    q"""
        new {} with JsonRpcFunction[$functionType] {
          override val function = $function

          override def dispose(): Try[Unit] = {
            Try(throw new UnsupportedOperationException("TODO: dispose the function"))
          }
        }
        """
  }

  def receive(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Future[Option[String]]] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val server = c.prefix.tree
    val jsonSerializer: Tree = q"$server.jsonSerializer"
    val methodNameToHandlerMap: Tree = q"$server.methodNameToHandlerMap"
    val executionContext: Tree = q"$server.executionContext"

    val maybeParseErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.parseError"))
    val maybeInvalidRequestErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidRequest"))
    val maybeMethodNotFoundErrorJson: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJson(server, json, c.Expr[JsonRpcError[String]](q"JsonRpcErrors.methodNotFound"))

    val maybeErrorJsonOrMethodName = c.Expr[Either[Option[String], String]](
      q"""
          $jsonSerializer.deserialize[JsonRpcMethod]($json)
              .toRight($maybeParseErrorJson)
              .right.flatMap(method => {
                if(method.jsonrpc != Constants.JsonRpc) {
                  Left($maybeInvalidRequestErrorJson)
                } else {
                  println("Received " + method.method)
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
