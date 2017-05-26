package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

object JSONRPCMethodClientResultFactory {
  def apply[Context <: blackbox.Context](c: Context) = new JSONRPCMethodClientResultFactory[c.type](c)
}

class JSONRPCMethodClientResultFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionClientFactoryMacro = new DisposableFunctionClientFactoryMacro[c.type](c)

  def create(
      client: Tree,
      maybeServer: Option[Tree],
      resultType: Type,
      resultResponse: Tree
  ): Tree = {
    val result = q"$resultResponse.result"
    if (macroUtils.isDisposableFunctionType(resultType)) {
      maybeServer
          .map(server => disposableFunctionClientFactoryMacro.getOrCreate(
            server = server,
            client = client,
            disposableFunctionMethodName = q"$result",
            disposableFunctionType = resultType
          ))
          .getOrElse(throw new UnsupportedOperationException("To create an API using DisposableFunction, you need to create the API with JSONRPCServerAndClient."))
    } else {
      result
    }
  }

  def createJSONRPCResultType(resultType: Type): Type = {
    if (macroUtils.isDisposableFunctionType(resultType)) {
      macroUtils.getType[String]
    } else {
      resultType
    }
  }
}
