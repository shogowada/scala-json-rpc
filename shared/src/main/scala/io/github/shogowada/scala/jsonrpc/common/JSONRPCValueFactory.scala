package io.github.shogowada.scala.jsonrpc.common

import io.github.shogowada.scala.jsonrpc.DisposableFunction
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
  private lazy val disposableFunctionClientFactory = new DisposableFunctionClientFactoryMacro[c.type](c)
  private lazy val disposableFunctionServerFactory = new DisposableFunctionServerFactoryMacro[c.type](c)

  def jsonRPCToScala(
      maybeClient: Option[Tree],
      maybeServer: Option[Tree],
      value: Tree,
      valueType: Type
  ): Tree = {
    if (isEitherType(valueType)) {
      val leftType: Type = valueType.typeArgs(0)
      val rightType: Type = valueType.typeArgs(1)
      val toScala = jsonRPCToScala(maybeClient, maybeServer, _: Tree, _: Type)
      q"""
          $value match {
            case Left(value) => Left(${toScala(q"value", leftType)})
            case Right(value) => Right(${toScala(q"value", rightType)})
          }
          """
    } else if (isDisposableFunctionType(valueType)) {
      val maybeValue = for {
        client <- maybeClient
        server <- maybeServer
      } yield disposableFunctionClientFactory.getOrCreate(
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

  def jsonRPCType(valueType: Type): Tree = {
    if (isEitherType(valueType)) {
      val leftType: Tree = jsonRPCType(valueType.typeArgs(0))
      val rightType: Tree = jsonRPCType(valueType.typeArgs(1))
      tq"Either[$leftType, $rightType]"
    } else if (isDisposableFunctionType(valueType)) {
      tq"String"
    } else {
      tq"$valueType"
    }
  }

  def scalaToJSONRPC(
      maybeClient: Option[Tree],
      maybeServer: Option[Tree],
      value: Tree,
      valueType: Type
  ): Tree = {
    if (isEitherType(valueType)) {
      val leftType = valueType.typeArgs(0)
      val rightType = valueType.typeArgs(1)
      val toJSONRPC = scalaToJSONRPC(maybeClient, maybeServer, _: Tree, _: Type)
      q"""
          $value match {
            case Left(value) => Left(${toJSONRPC(q"value", leftType)})
            case Right(value) => Right(${toJSONRPC(q"value", rightType)})
          }
          """
    } else if (isDisposableFunctionType(valueType)) {
      val maybeValue: Option[c.Expr[String]] = for {
        server <- maybeServer
        client <- maybeClient
      } yield disposableFunctionServerFactory.getOrCreate(
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

  private def isEitherType(valueType: Type): Boolean = {
    valueType <:< macroUtils.getType[Either[_, _]]
  }

  private def isDisposableFunctionType(valueType: Type): Boolean = {
    valueType <:< macroUtils.getType[DisposableFunction]
  }

  private def DisposableFunctionException: Throwable =
    new UnsupportedOperationException("To use DisposableFunction, you need to use JSONRPCServerAndClient.")
}
