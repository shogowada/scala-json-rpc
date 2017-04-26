package io.github.shogowada.scala.jsonrpc

object Constants {
  final val JSONRPC = "2.0"

  final val ReservedMethodNamePrefix = "io.github.shogowada.scala.jsonrpc."

  final val FunctionMethodNamePrefix = ReservedMethodNamePrefix + "function."

  final val DisposeMethodName = ReservedMethodNamePrefix + "dispose"
}
