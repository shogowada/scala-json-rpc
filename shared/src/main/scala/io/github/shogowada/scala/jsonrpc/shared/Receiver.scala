package io.github.shogowada.scala.jsonrpc.shared

trait Receiver {
  def receive(json: String): Unit
}
