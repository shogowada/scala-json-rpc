package io.github.shogowada.scala.jsonrpc

object Constants {
  final val JSONRPC: String = "2.0"

  final val ReservedMethodNamePrefix: String = "io.github.shogowada.scala.jsonrpc."

  final val FunctionMethodNamePrefix: String = ReservedMethodNamePrefix + "function."

  final val DisposeMethodName: String = ReservedMethodNamePrefix + "dispose"
}
