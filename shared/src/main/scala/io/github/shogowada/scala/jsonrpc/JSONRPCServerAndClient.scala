package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.client.{JSONRPCClient, JSONRPCClientMacro}
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.server.{JSONRPCServer, JSONRPCServerMacro}
import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JSONRPCServerAndClient[JsonSerializerInUse <: JsonSerializer](
    val server: JSONRPCServer[JsonSerializerInUse],
    val client: JSONRPCClient[JsonSerializerInUse]
) {
  def send(json: String): Future[Option[String]] = client.send(json)

  def bindAPI[API](api: API): Unit = macro JSONRPCServerAndClientMacro.bindAPI[API]

  def createAPI[API]: API = macro JSONRPCServerAndClientMacro.createAPI[API]

  def receiveAndSend(json: String): Future[Unit] = macro JSONRPCServerAndClientMacro.receiveAndSend
}

object JSONRPCServerAndClient {
  def apply[JsonSerializerInUse <: JsonSerializer](
      server: JSONRPCServer[JsonSerializerInUse],
      client: JSONRPCClient[JsonSerializerInUse]
  ) = new JSONRPCServerAndClient(server, client)
}

object JSONRPCServerAndClientMacro {
  def bindAPI[API: c.WeakTypeTag](c: blackbox.Context)(api: c.Expr[API]): c.Expr[Unit] = {
    import c.universe._
    val macroUtils = JSONRPCMacroUtils[c.type](c)
    val (serverAndClientDefinition, serverAndClient) = macroUtils.prefixDefinitionAndReference
    val server = q"$serverAndClient.server"
    val client = q"$serverAndClient.client"
    val bindAPI = JSONRPCServerMacro.bindAPIImpl[c.type, API](c)(server, Some(client), api)
    c.Expr[Unit](
      q"""
          $serverAndClientDefinition
          $bindAPI
          """
    )
  }

  def createAPI[API: c.WeakTypeTag](c: blackbox.Context): c.Expr[API] = {
    import c.universe._
    val macroUtils = JSONRPCMacroUtils[c.type](c)
    val (serverAndClientDefinition, serverAndClient) = macroUtils.prefixDefinitionAndReference
    val server = q"$serverAndClient.server"
    val client = q"$serverAndClient.client"
    val createAPI = JSONRPCClientMacro.createAPIImpl[c.type, API](c)(client, Some(server))
    c.Expr[API](
      q"""
          $serverAndClientDefinition
          $createAPI
          """
    )
  }

  def receiveAndSend(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Future[Unit]] = {
    import c.universe._
    val macroUtils = JSONRPCMacroUtils[c.type](c)
    val (serverAndClientDefinition, serverAndClient) = macroUtils.prefixDefinitionAndReference
    val server: Tree = q"$serverAndClient.server"
    val client: Tree = q"$serverAndClient.client"
    val executionContext: c.Expr[ExecutionContext] = c.Expr(q"$server.executionContext")
    c.Expr[Future[Unit]](
      q"""
          ..${macroUtils.imports}
          $serverAndClientDefinition
          def receiveAndSend(json: String): Future[Unit] = {
            val wasJSONRPCResponse: Boolean = $client.receive(json)
            if (!wasJSONRPCResponse) {
              $server.receive(json)
                .flatMap((maybeResponseJsonFromUs: Option[String]) => {
                  maybeResponseJsonFromUs match {
                    case Some(responseJsonFromUs) => $client.send(responseJsonFromUs)
                    case None => Future(None)($executionContext)
                  }
                })($executionContext)
                .flatMap((maybeResponseJsonFromThem: Option[String]) => {
                  maybeResponseJsonFromThem match {
                    case Some(responseJsonFromThem) => receiveAndSend(responseJsonFromThem)
                    case None => Future(None)($executionContext)
                  }
                })($executionContext)
                .map(_ => ())
            } else {
              Future()($executionContext)
            }
          }
          receiveAndSend($json)
          """
    )
  }
}
