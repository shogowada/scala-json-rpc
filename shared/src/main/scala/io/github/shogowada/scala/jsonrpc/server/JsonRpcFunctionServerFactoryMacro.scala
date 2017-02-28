package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcFunctionServerFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val requestJsonHandlerFactoryMacro = new JsonRpcRequestJsonHandlerFactoryMacro[c.type](c)

  def getOrCreate(
      client: Tree,
      server: Tree,
      jsonRpcFunction: TermName,
      jsonRpcFunctionType: Type
  ): c.Expr[String] = {
    val requestJsonHandlerRepository = macroUtils.getRequestJsonHandlerRepository(server)
    val jsonRpcFunctionMethodNameRepository = macroUtils.getJsonRpcFunctionMethodNameRepository(client)

    val disposeFunctionMethodHandler = requestJsonHandlerFactoryMacro.createDisposeFunctionMethodHandler(server, client)

    val handler = requestJsonHandlerFactoryMacro.createFromJsonRpcFunction(client, server, jsonRpcFunction, jsonRpcFunctionType)

    c.Expr[String](
      q"""
          $requestJsonHandlerRepository.addIfAbsent(Constants.DisposeMethodName, () => ($disposeFunctionMethodHandler))

          val methodName: String = $jsonRpcFunctionMethodNameRepository.getOrAddAndNotify(
            $jsonRpcFunction,
            (newMethodName) => { $requestJsonHandlerRepository.add(newMethodName, $handler) }
          )
          methodName
          """
    )
  }
}
