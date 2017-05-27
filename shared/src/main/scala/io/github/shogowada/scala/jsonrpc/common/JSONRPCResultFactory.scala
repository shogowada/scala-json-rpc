package io.github.shogowada.scala.jsonrpc.common

import scala.reflect.macros.blackbox

object JSONRPCResultFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCResultFactory[c.type] =
    new JSONRPCResultFactory[c.type](c)
}

class JSONRPCResultFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val valueFactory = JSONRPCValueFactory[c.type](c)

  def jsonRPCToScala(
      client: Tree,
      maybeServer: Option[Tree],
      result: Tree,
      resultType: Type
  ): Tree = {
    valueFactory.jsonRPCToScala(
      maybeClient = Option(client),
      maybeServer = maybeServer,
      value = result,
      valueType = resultType
    )
  }

  def jsonRPCType(resultType: Type): Tree = {
    valueFactory.jsonRPCType(resultType)
  }

  def scalaToJSONRPC(
      server: Tree,
      maybeClient: Option[Tree],
      result: Tree,
      resultType: Type
  ): Tree = {
    valueFactory.scalaToJSONRPC(
      maybeServer = Option(server),
      maybeClient = maybeClient,
      value = result,
      valueType = resultType
    )
  }
}
