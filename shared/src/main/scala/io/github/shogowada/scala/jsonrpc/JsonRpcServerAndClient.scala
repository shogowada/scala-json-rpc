package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.client.{JsonRpcClient, JsonRpcClientMacro}
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.{JsonRpcServer, JsonRpcServerMacro}
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcServerAndClient[JSON_SERIALIZER <: JsonSerializer](
    val server: JsonRpcServer[JSON_SERIALIZER],
    val client: JsonRpcClient[JSON_SERIALIZER]
) {
  def send(json: String): Future[Option[String]] = client.send(json)

  def bindApi[API](api: API): Unit = macro JsonRpcServerAndClientMacro.bindApi[API]

  def createApi[API]: API = macro JsonRpcServerAndClientMacro.createApi[API]

  def receive(json: String): Unit = macro JsonRpcServerAndClientMacro.receive
}

object JsonRpcServerAndClient {
  def apply[JSON_SERIALIZER <: JsonSerializer](
      server: JsonRpcServer[JSON_SERIALIZER],
      client: JsonRpcClient[JSON_SERIALIZER]
  ) = new JsonRpcServerAndClient(server, client)
}

object JsonRpcServerAndClientMacro {
  def bindApi[API: c.WeakTypeTag](c: blackbox.Context)(api: c.Expr[API]): c.Expr[Unit] = {
    import c.universe._
    val server = q"${c.prefix.tree}.server"
    val client = q"${c.prefix.tree}.client"
    JsonRpcServerMacro.bindApiImpl[c.type, API](c)(server, Some(client), api)
  }

  def createApi[API: c.WeakTypeTag](c: blackbox.Context): c.Expr[API] = {
    import c.universe._
    val server = q"${c.prefix.tree}.server"
    val client = q"${c.prefix.tree}.client"
    JsonRpcClientMacro.createApiImpl[c.type, API](c)(client, Some(server))
  }

  def receive(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Unit] = {
    import c.universe._
    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val client: Tree = q"${c.prefix.tree}.client"
    val server: Tree = q"${c.prefix.tree}.server"
    val executionContext: c.Expr[ExecutionContext] = c.Expr(q"$server.executionContext")
    c.Expr[Unit](
      q"""
          ..${macroUtils.imports}
          val wasJsonRpcResponse: Boolean = $client.receive($json)
          if (!wasJsonRpcResponse) {
            $server.receive($json)
              .onComplete((tried: Try[Option[String]]) => tried match {
                case Success(Some(responseJson: String)) => $client.send(responseJson)
                case _ =>
              })($executionContext)
          }
          """
    )
  }
}
