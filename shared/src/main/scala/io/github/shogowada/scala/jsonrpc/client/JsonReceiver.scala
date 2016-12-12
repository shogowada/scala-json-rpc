package io.github.shogowada.scala.jsonrpc.client

trait JsonReceiver {
  def receive(json: String): Unit
}
