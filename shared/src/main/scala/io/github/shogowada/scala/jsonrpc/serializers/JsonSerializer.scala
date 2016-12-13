package io.github.shogowada.scala.jsonrpc.serializers

import scala.language.higherKinds

trait JsonSerializer[SERIALIZER[_], DESERIALIZER[_]] {
  def serialize[T: SERIALIZER](value: T): Option[String]

  def deserialize[T: DESERIALIZER](json: String): Option[T]
}
