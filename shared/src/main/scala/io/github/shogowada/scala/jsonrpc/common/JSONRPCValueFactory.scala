package io.github.shogowada.scala.jsonrpc.common

import io.github.shogowada.scala.jsonrpc.client.DisposableFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.server.DisposableFunctionServerFactoryMacro

import scala.reflect.macros.blackbox

object JSONRPCValueFactory {
  def apply[Context <: blackbox.Context](c: Context): JSONRPCValueFactory[c.type] =
    new JSONRPCValueFactory[c.type](c)
}

class JSONRPCValueFactory[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionClientFactoryMacro = new DisposableFunctionClientFactoryMacro[c.type](c)
  private lazy val disposableFunctionServerFactory = new DisposableFunctionServerFactoryMacro[c.type](c)

  def jsonRPCToScala(
      maybeClient: Option[Tree],
      maybeServer: Option[Tree],
      value: Tree,
      valueType: Type
  ): Tree = {
    if (macroUtils.isDisposableFunctionType(valueType)) {
      val maybeValue = for (
        client <- maybeClient;
        server <- maybeServer
      ) yield disposableFunctionClientFactoryMacro.getOrCreate(
        server = server,
        client = client,
        disposableFunctionMethodName = value,
        disposableFunctionType = valueType
      )

      maybeValue.getOrElse(throw DisposableFunctionException)
    } else {
      value
    }
  }

  def jsonRPCType(resultType: Type): Type = {
    if (macroUtils.isDisposableFunctionType(resultType)) {
      macroUtils.getType[String]
    } else {
      resultType
    }
  }

  def scalaToJSONRPC(
      maybeServer: Option[Tree],
      maybeClient: Option[Tree],
      value: Tree,
      valueType: Type
  ): Tree = {
    if (macroUtils.isDisposableFunctionType(valueType)) {
      val maybeValue = for (
        server <- maybeServer;
        client <- maybeClient
      ) yield disposableFunctionServerFactory.getOrCreate(
        server = server,
        client = client,
        disposableFunction = value,
        disposableFunctionType = valueType
      )

      maybeValue
          .map(value => q"$value")
          .getOrElse(throw DisposableFunctionException)
    } else {
      value
    }
  }

  private def DisposableFunctionException: Throwable =
    new UnsupportedOperationException("To use DisposableFunction, you need to bind the API to JSONRPCServerAndClient.")
}
