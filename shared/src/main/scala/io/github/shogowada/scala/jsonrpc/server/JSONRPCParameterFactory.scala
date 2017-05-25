package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.client.DisposableFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

object JSONRPCParameterFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCParameterFactory[c.type] =
    new JSONRPCParameterFactory[c.type](c)
}

class JSONRPCParameterFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionClientFactory = new DisposableFunctionClientFactoryMacro[c.type](c)

  def create(
      server: Tree,
      maybeClient: Option[Tree],
      argument: Tree,
      argumentType: Type
  ): Tree = {
    if (macroUtils.isDisposableFunctionType(argumentType)) {
      maybeClient
          .map(client => disposableFunctionClientFactory.getOrCreate(
            server = server,
            client = client,
            disposableFunctionType = argumentType,
            disposableFunctionMethodName = argument
          ))
          .getOrElse(throw new UnsupportedOperationException("To use DisposableFunction, you need to bind the API to JSONRPCServerAndClient."))
    } else {
      argument
    }
  }
}
