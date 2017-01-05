package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

class RandomNumberReceiverApiImpl extends RandomNumberReceiverApi {
  override def receive(randomNumber: Int): Unit = {
    println(s"Received random number $randomNumber")
  }
}
