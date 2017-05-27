package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.common.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

class DisposableFunctionServerFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val requestJSONHandlerFactoryMacro = new JSONRPCRequestJSONHandlerFactoryMacro[c.type](c)

  def getOrCreate(
      client: Tree,
      server: Tree,
      disposableFunction: Tree,
      disposableFunctionType: Type
  ): Tree = {
    val requestJSONHandlerRepository = macroUtils.getRequestJSONHandlerRepository(server)
    val disposableFunctionMethodNameRepository = macroUtils.getDisposableFunctionMethodNameRepository(client)

    val disposeFunctionMethodHandler = requestJSONHandlerFactoryMacro.createDisposeFunctionMethodHandler(server, client)

    val handler = requestJSONHandlerFactoryMacro.createFromDisposableFunction(client, server, disposableFunction, disposableFunctionType)

    q"""
        $requestJSONHandlerRepository.addIfAbsent(Constants.DisposeMethodName, () => ($disposeFunctionMethodHandler))

        val methodName: String = $disposableFunctionMethodNameRepository.getOrAddAndNotify(
          $disposableFunction,
          (newMethodName) => { $requestJSONHandlerRepository.add(newMethodName, $handler) }
        )
        methodName
        """
  }
}
