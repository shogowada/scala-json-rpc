package io.github.shogowada.scala.jsonrpc.communicators

trait Sender {
  def send(json: String): Unit
}
