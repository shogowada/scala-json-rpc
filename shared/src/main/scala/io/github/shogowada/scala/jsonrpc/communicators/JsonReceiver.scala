package io.github.shogowada.scala.jsonrpc.communicators

trait JsonReceiver {
  def receive(json: String): Unit
}
