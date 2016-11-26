package io.github.shogowada.scala.jsonrpc.communicators

trait JsonSender {
  def send(json: String): Unit
}
