package io.github.shogowada.scala.jsonrpc.common

import io.github.shogowada.scala.jsonrpc.client.DisposableFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.server.DisposableFunctionServerFactoryMacro

import scala.reflect.macros.blackbox

object JSONRPCResultFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCResultFactory[c.type] =
    new JSONRPCResultFactory[c.type](c)
}

class JSONRPCResultFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionClientFactoryMacro = new DisposableFunctionClientFactoryMacro[c.type](c)
  private lazy val disposableFunctionServerFactory = new DisposableFunctionServerFactoryMacro[c.type](c)

  def jsonRPCToScala(
      client: Tree,
      maybeServer: Option[Tree],
      result: Tree,
      resultType: Type
  ): Tree = {
    if (macroUtils.isDisposableFunctionType(resultType)) {
      maybeServer
          .map(server => disposableFunctionClientFactoryMacro.getOrCreate(
            server = server,
            client = client,
            disposableFunctionMethodName = result,
            disposableFunctionType = resultType
          ))
          .getOrElse(throw new UnsupportedOperationException("To create an API using DisposableFunction, you need to create the API with JSONRPCServerAndClient."))
    } else {
      result
    }
  }

  def jsonRPCType(resultType: Type): Type = {
    if (macroUtils.isDisposableFunctionType(resultType)) {
      macroUtils.getType[String]
    } else {
      resultType
    }
  }

  def scalaToJSONRPC(
      server: Tree,
      maybeClient: Option[Tree],
      result: Tree,
      resultType: Type
  ): Tree = {
    if (macroUtils.isDisposableFunctionType(resultType)) {
      val disposableFunctionServer: c.Expr[String] = maybeClient
          .map(client => disposableFunctionServerFactory.getOrCreate(
            server = server,
            client = client,
            disposableFunction = result,
            disposableFunctionType = resultType
          ))
          .getOrElse(throw new UnsupportedOperationException("To return DisposableFunction, you need to bind the API to JSONRPCServerAndClient."))

      q"$disposableFunctionServer"
    } else {
      result
    }
  }
}
