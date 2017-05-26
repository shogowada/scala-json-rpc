package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.server.DisposableFunctionServerFactoryMacro
import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

object JSONRPCParameterFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCParameterFactory[c.type] =
    new JSONRPCParameterFactory[c.type](c)
}

class JSONRPCParameterFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionServerFactoryMacro = new DisposableFunctionServerFactoryMacro[c.type](c)

  def create(
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
