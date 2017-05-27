package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JSONRPCError
import io.github.shogowada.scala.jsonrpc.serializers.JSONSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer.RequestJSONHandler
import io.github.shogowada.scala.jsonrpc.common.JSONRPCMacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JSONRPCServer[JSONSerializerInUse <: JSONSerializer](
    val jsonSerializer: JSONSerializerInUse,
    val executionContext: ExecutionContext
) {
  val requestJSONHandlerRepository = new JSONRPCRequestJSONHandlerRepository
  val disposableFunctionRepository = new DisposableFunctionRepository

  def bindAPI[API](api: API): Unit = macro JSONRPCServerMacro.bindAPI[API]

  def receive(json: String): Future[Option[String]] = macro JSONRPCServerMacro.receive
}

object JSONRPCServer {
  type RequestJSONHandler = (String) => Future[Option[String]]

  def apply[JSONSerializerInUse <: JSONSerializer](
      jsonSerializer: JSONSerializerInUse
  )(implicit executionContext: ExecutionContext): JSONRPCServer[JSONSerializerInUse] = {
    new JSONRPCServer(jsonSerializer, executionContext)
  }
}

object JSONRPCServerMacro {
  def bindAPI[API: c.WeakTypeTag](c: blackbox.Context)(api: c.Expr[API]): c.Expr[Unit] = {
    import c.universe._
    val macroUtils = JSONRPCMacroUtils[c.type](c)
    val (serverDefinition, server) = macroUtils.prefixDefinitionAndReference
    val bind = bindAPIImpl[c.type, API](c)(server, None, api)
    c.Expr[Unit](
      q"""
          {
            $serverDefinition
            $bind
          }
          """
    )
  }

  def bindAPIImpl[Context <: blackbox.Context, API: c.WeakTypeTag](c: Context)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API]
  ): c.Expr[Unit] = {
    import c.universe._

    val macroUtils = JSONRPCMacroUtils[c.type](c)

    val requestJSONHandlerRepository = macroUtils.getRequestJSONHandlerRepository(server)

    val apiType: Type = weakTypeOf[API]
    val methodNameToRequestJSONHandlerList = JSONRPCMacroUtils[c.type](c).getJSONRPCAPIMethods(apiType)
        .map((apiMember: MethodSymbol) => createMethodNameToRequestJSONHandler[c.type, API](c)(server, maybeClient, api, apiMember))

    c.Expr[Unit](
      q"""
          Seq(..$methodNameToRequestJSONHandlerList).foreach {
            case (methodName, handler) => $requestJSONHandlerRepository.add(methodName, handler)
          }
          """
    )
  }

  private def createMethodNameToRequestJSONHandler[Context <: blackbox.Context, API](c: blackbox.Context)(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[(String, RequestJSONHandler)] = {
    import c.universe._

    val macroUtils = JSONRPCMacroUtils[c.type](c)
    val requestJSONHandlerFactoryMacro = new JSONRPCRequestJSONHandlerFactoryMacro[c.type](c)

    val methodName = macroUtils.getJSONRPCMethodName(method)
    val handler = requestJSONHandlerFactoryMacro.createFromAPIMethod[API](server, maybeClient, api, method)

    c.Expr[(String, RequestJSONHandler)](q"""$methodName -> $handler""")
  }

  def receive(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Future[Option[String]]] = {
    import c.universe._

    val macroUtils = JSONRPCMacroUtils[c.type](c)

    val (serverDefinition, server) = macroUtils.prefixDefinitionAndReference
    val jsonSerializer: Tree = macroUtils.getJSONSerializer(server)
    val requestJSONHandlerRepository = macroUtils.getRequestJSONHandlerRepository(server)
    val executionContext: Tree = macroUtils.getExecutionContext(server)

    val maybeParseErrorJSON: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJSONFromRequestJSON(server, json, c.Expr[JSONRPCError[String]](q"JSONRPCErrors.parseError"))
    val maybeInvalidRequestErrorJSON: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJSONFromRequestJSON(server, json, c.Expr[JSONRPCError[String]](q"JSONRPCErrors.invalidRequest"))
    val maybeMethodNotFoundErrorJSON: c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJSONFromRequestJSON(server, json, c.Expr[JSONRPCError[String]](q"JSONRPCErrors.methodNotFound"))

    val maybeErrorJSONOrMethodName = c.Expr[Either[Option[String], String]](
      q"""
          $jsonSerializer.deserialize[JSONRPCMethod]($json)
              .toRight($maybeParseErrorJSON)
              .right.flatMap(method => {
                if(method.jsonrpc != Constants.JSONRPC) {
                  Left($maybeInvalidRequestErrorJSON)
                } else {
                  Right(method.method)
                }
              })
          """
    )

    val maybeErrorJSONOrHandler = c.Expr[Either[Option[String], RequestJSONHandler]](
      q"""
          $maybeErrorJSONOrMethodName
              .right.flatMap((methodName: String) => {
                $requestJSONHandlerRepository.get(methodName)
                  .toRight($maybeMethodNotFoundErrorJSON)
              })
          """
    )

    val futureMaybeJSON = c.Expr[Future[Option[String]]](
      q"""
          $maybeErrorJSONOrHandler.fold[Future[Option[String]]](
            maybeErrorJSON => Future(maybeErrorJSON)($executionContext),
            handler => handler($json)
          )
          """
    )

    c.Expr(
      q"""
          {
            ..${macroUtils.imports}
            $serverDefinition
            $futureMaybeJSON
          }
          """
    )
  }
}
