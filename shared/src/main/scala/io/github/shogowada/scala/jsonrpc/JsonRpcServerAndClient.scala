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

  def receiveAndSend(json: String): Future[Unit] = macro JsonRpcServerAndClientMacro.receiveAndSend
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
    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val (serverAndClientDefinition, serverAndClient) = macroUtils.prefixDefinitionAndReference
    val server = q"$serverAndClient.server"
    val client = q"$serverAndClient.client"
    val bindApi = JsonRpcServerMacro.bindApiImpl[c.type, API](c)(server, Some(client), api)
    c.Expr[Unit](
      q"""
          $serverAndClientDefinition
          $bindApi
          """
    )
  }

  def createApi[API: c.WeakTypeTag](c: blackbox.Context): c.Expr[API] = {
    import c.universe._
    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val (serverAndClientDefinition, serverAndClient) = macroUtils.prefixDefinitionAndReference
    val server = q"$serverAndClient.server"
    val client = q"$serverAndClient.client"
    val createApi = JsonRpcClientMacro.createApiImpl[c.type, API](c)(client, Some(server))
    c.Expr[API](
      q"""
          $serverAndClientDefinition
          $createApi
          """
    )
  }

  def receiveAndSend(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Future[Unit]] = {
    import c.universe._
    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val (serverAndClientDefinition, serverAndClient) = macroUtils.prefixDefinitionAndReference
    val server: Tree = q"$serverAndClient.server"
    val client: Tree = q"$serverAndClient.client"
    val executionContext: c.Expr[ExecutionContext] = c.Expr(q"$server.executionContext")
    c.Expr[Future[Unit]](
      q"""
          ..${macroUtils.imports}
          $serverAndClientDefinition
          def receiveAndSend(json: String): Future[Unit] = {
            val wasJsonRpcResponse: Boolean = $client.receive(json)
            if (!wasJsonRpcResponse) {
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
