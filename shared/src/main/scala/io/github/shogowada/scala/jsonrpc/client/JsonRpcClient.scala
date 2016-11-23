package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.communicators.{Receiver, Sender}
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}

class JsonRpcClient
(
    sender: Sender,
    jsonSerializer: JsonSerializer,
    jsonDeserializer: JsonDeserializer
) extends Receiver {
  def createApiClient[T]: T = {
    throw new UnsupportedOperationException
  }

  override def receive(json: String): Unit = {
    throw new UnsupportedOperationException
  }
}
