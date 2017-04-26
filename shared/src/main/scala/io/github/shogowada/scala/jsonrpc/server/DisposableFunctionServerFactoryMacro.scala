package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

class DisposableFunctionServerFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  lazy val requestJsonHandlerFactoryMacro = new JSONRPCRequestJsonHandlerFactoryMacro[c.type](c)

  def getOrCreate(
      client: Tree,
      server: Tree,
      disposableFunction: TermName,
      disposableFunctionType: Type
  ): c.Expr[String] = {
    val requestJsonHandlerRepository = macroUtils.getRequestJsonHandlerRepository(server)
    val disposableFunctionMethodNameRepository = macroUtils.getDisposableFunctionMethodNameRepository(client)

    val disposeFunctionMethodHandler = requestJsonHandlerFactoryMacro.createDisposeFunctionMethodHandler(server, client)

    val handler = requestJsonHandlerFactoryMacro.createFromDisposableFunction(client, server, disposableFunction, disposableFunctionType)

    c.Expr[String](
      q"""
          $requestJsonHandlerRepository.addIfAbsent(Constants.DisposeMethodName, () => ($disposeFunctionMethodHandler))

          val methodName: String = $disposableFunctionMethodNameRepository.getOrAddAndNotify(
            $disposableFunction,
            (newMethodName) => { $requestJsonHandlerRepository.add(newMethodName, $handler) }
          )
          methodName
          """
    )
  }
}
