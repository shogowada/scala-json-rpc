package io.github.shogowada.scala.jsonrpc.serializers

trait JsonSerializer {
  def serialize[T](value: T): Option[String]

  def deserialize[T](json: String): Option[T]
}
