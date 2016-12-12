package io.github.shogowada.scala.jsonrpc.client

trait JsonSender {
  def send(json: String): Unit
}
