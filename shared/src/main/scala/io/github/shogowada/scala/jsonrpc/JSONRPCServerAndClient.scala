package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.client.{JSONRPCClient, JSONRPCClientMacro}
import io.github.shogowada.scala.jsonrpc.serializers.JSONSerializer
import io.github.shogowada.scala.jsonrpc.server.{JSONRPCServer, JSONRPCServerMacro}
import io.github.shogowada.scala.jsonrpc.common.JSONRPCMacroUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JSONRPCServerAndClient[JSONSerializerInUse <: JSONSerializer](
    val server: JSONRPCServer[JSONSerializerInUse],
    val client: JSONRPCClient[JSONSerializerInUse]
) {
  def send(json: String): Future[Option[String]] = client.send(json)

  def bindAPI[API](api: API): Unit = macro JSONRPCServerAndClientMacro.bindAPI[API]

  def createAPI[API]: API = macro JSONRPCServerAndClientMacro.createAPI[API]

  def receiveAndSend(json: String): Future[Unit] = macro JSONRPCServerAndClientMacro.receiveAndSend
}

object JSONRPCServerAndClient {
  def apply[JSONSerializerInUse <: JSONSerializer](
      server: JSONRPCServer[JSONSerializerInUse],
      client: JSONRPCClient[JSONSerializerInUse]
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
          {
            $serverAndClientDefinition
            $bindAPI
          }
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
          {
            $serverAndClientDefinition
            $createAPI
          }
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
          {
            ..${macroUtils.imports}
            $serverAndClientDefinition
            def receiveAndSend(json: String): Future[Unit] = {
              val wasJSONRPCResponse: Boolean = $client.receive(json)
              if (!wasJSONRPCResponse) {
                $server.receive(json)
                  .flatMap((maybeResponseJSONFromUs: Option[String]) => {
                    maybeResponseJSONFromUs match {
                      case Some(responseJSONFromUs) => $client.send(responseJSONFromUs)
                      case None => Future(None)($executionContext)
                    }
                  })($executionContext)
                  .flatMap((maybeResponseJSONFromThem: Option[String]) => {
                    maybeResponseJSONFromThem match {
                      case Some(responseJSONFromThem) => receiveAndSend(responseJSONFromThem)
                      case None => Future(None)($executionContext)
                    }
                  })($executionContext)
                  .map(_ => ())
              } else {
                Future()($executionContext)
              }
            }
            receiveAndSend($json)
          }
          """
    )
  }
}
