package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.client.JsonRpcMethodClientMacroFactory
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcFunctionServerMacroFactory[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val methodClientMacroFactory = new JsonRpcMethodClientMacroFactory[c.type](c)

  def getOrCreateJsonRpcFunction(
      server: c.Tree,
      client: c.Tree,
      jsonRpcFunctionType: c.Type,
      jsonRpcFunctionMethodName: c.Tree
  ): c.Tree = {
    val newJsonRpcFunction = createJsonRpcFunction(server, client, jsonRpcFunctionType, jsonRpcFunctionMethodName)

    q"""
        $newJsonRpcFunction
        """
  }

  private def createJsonRpcFunction(
      server: c.Tree,
      client: c.Tree,
      jsonRpcFunctionType: c.Type,
      jsonRpcFunctionMethodName: c.Tree
  ): c.Tree = {
    val functionType: Type = macroUtils.getFunctionTypeOfJsonRpcFunctionType(jsonRpcFunctionType)
    val functionTypeTypeArgs: Seq[Type] = functionType.typeArgs
    val paramTypes: Seq[Type] = functionTypeTypeArgs.init
    val returnType: Type = functionTypeTypeArgs.last
    val function = methodClientMacroFactory.createMethodClientAsFunction(client, Some(server), jsonRpcFunctionMethodName, paramTypes, returnType)

    q"""
        new {} with JsonRpcFunction[$functionType] {
          override val function = $function

          override def dispose(): Future[Unit] = {
            Future.failed(new UnsupportedOperationException("TODO: dispose the function"))
          }
        }
        """
  }
}
