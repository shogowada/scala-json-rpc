package io.github.shogowada.scala.jsonrpc.serializers

trait JsonDeserializer {
  def deserialize[T](json: String): Option[T]
}
