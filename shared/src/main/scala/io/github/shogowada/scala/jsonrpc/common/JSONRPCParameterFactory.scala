package io.github.shogowada.scala.jsonrpc.common

import scala.reflect.macros.blackbox

object JSONRPCParameterFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCParameterFactory[c.type] =
    new JSONRPCParameterFactory[c.type](c)
}

class JSONRPCParameterFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val valueFactory = JSONRPCValueFactory[c.type](c)

  def jsonRPCToScala(
      server: Tree,
      maybeClient: Option[Tree],
      argument: Tree,
      argumentType: Type
  ): Tree = {
    valueFactory.jsonRPCToScala(
      maybeClient = maybeClient,
      maybeServer = Option(server),
      argument,
      argumentType
    )
  }

  def jsonRPCType(paramTypes: Seq[c.Type]): Tree = {
    val parameterTypes: Iterable[Type] = paramTypes
        .map(jsonRPCType)

    if (parameterTypes.size == 1) {
      val parameterType = parameterTypes.head
      tq"Tuple1[$parameterType]"
    } else {
      tq"(..$parameterTypes)"
    }
  }

  private def jsonRPCType(paramType: Type): Type = {
    valueFactory.jsonRPCType(paramType)
  }

  def scalaToJSONRPC(
      client: Tree,
      maybeServer: Option[Tree],
      parameter: Tree,
      parameterType: Type
  ): Tree = {
    valueFactory.scalaToJSONRPC(
      maybeServer = maybeServer,
      maybeClient = Option(client),
      value = parameter,
      valueType = parameterType
    )
  }
}
