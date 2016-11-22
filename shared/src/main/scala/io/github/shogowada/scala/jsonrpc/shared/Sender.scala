package io.github.shogowada.scala.jsonrpc.shared

trait Sender {
  def send(json: String): Unit
}
