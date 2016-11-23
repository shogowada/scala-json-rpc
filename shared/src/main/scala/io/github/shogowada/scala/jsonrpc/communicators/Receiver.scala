package io.github.shogowada.scala.jsonrpc.communicators

trait Receiver {
  def receive(json: String): Unit
}
