package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

object JSONRPCResultFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCResultFactory[c.type] =
    new JSONRPCResultFactory[c.type](c)
}

class JSONRPCResultFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionServerFactory = new DisposableFunctionServerFactoryMacro[c.type](c)

  def create(
      server: Tree,
      maybeClient: Option[Tree],
      returnValue: Tree,
      returnValueType: Type
  ): Tree = {
    val executionContext = macroUtils.getExecutionContext(server)

    val wrappedReturnValueType: Type = returnValueType.typeArgs.head
    if (macroUtils.isDisposableFunctionType(wrappedReturnValueType)) {

      val disposableFunctionServer = maybeClient
          .map(client => disposableFunctionServerFactory.getOrCreate(
            server = server,
            client = client,
            disposableFunction = TermName("disposableFunction"),
            disposableFunctionType = wrappedReturnValueType
          ))
          .getOrElse(throw new UnsupportedOperationException("To return DisposableFunction, you need to bind the API to JSONRPCServerAndClient."))

      q"""
          $returnValue
              .map(disposableFunction => $disposableFunctionServer)($executionContext)
          """
    } else {
      returnValue
    }
  }
}
