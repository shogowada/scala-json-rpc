package io.github.shogowada.scala.jsonrpc.server

trait JsonRpcMethodRepository {
  def bind[T](apiFactory: () => T): Unit

  def get(method: String)
}
