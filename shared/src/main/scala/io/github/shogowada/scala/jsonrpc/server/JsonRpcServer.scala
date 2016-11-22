package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.shared.Receiver

class JsonRpcServer extends Receiver {
  def bindApi[T](api: T): Unit = bindApi(() => api)

  def bindApi[T](apiFactory: () => T): Unit = {
    throw new UnsupportedOperationException
  }

  override def receive(json: String): Unit = {
    throw new UnsupportedOperationException
  }
}
