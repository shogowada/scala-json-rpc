package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

trait RandomNumberSubscriberApi {
  def subscribe(): Unit

  def unsubscribe(): Unit
}

trait RandomNumberReceiverApi {
  def receive(randomNumber: Int): Unit
}
