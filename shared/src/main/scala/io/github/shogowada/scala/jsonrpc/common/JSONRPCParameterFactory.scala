package io.github.shogowada.scala.jsonrpc.common

import io.github.shogowada.scala.jsonrpc.client.DisposableFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.server.DisposableFunctionServerFactoryMacro

import scala.reflect.macros.blackbox

object JSONRPCParameterFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCParameterFactory[c.type] =
    new JSONRPCParameterFactory[c.type](c)
}

class JSONRPCParameterFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionClientFactory = new DisposableFunctionClientFactoryMacro[c.type](c)
  private lazy val disposableFunctionServerFactoryMacro = new DisposableFunctionServerFactoryMacro[c.type](c)

  def jsonRPCToScala(
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
            disposableFunctionMethodName = argument,
            disposableFunctionType = argumentType
          ))
          .getOrElse(throw new UnsupportedOperationException("To use DisposableFunction, you need to bind the API to JSONRPCServerAndClient."))
    } else {
      argument
    }
  }

  def scalaToJSONRPC(
      client: Tree,
      maybeServer: Option[Tree],
      parameter: TermName,
      parameterType: Type
  ): Tree = {
    if (macroUtils.isDisposableFunctionType(parameterType)) {
      val disposableFunctionMethodName: c.Expr[String] = maybeServer
          .map(server => disposableFunctionServerFactoryMacro.getOrCreate(
            client = client,
            server = server,
            disposableFunction = parameter,
            disposableFunctionType = parameterType
          ))
          .getOrElse(throw new UnsupportedOperationException("To use DisposableFunction, you need to create an API with JSONRPCServerAndClient."))
      q"$disposableFunctionMethodName"
    } else {
      q"$parameter"
    }
  }
}
