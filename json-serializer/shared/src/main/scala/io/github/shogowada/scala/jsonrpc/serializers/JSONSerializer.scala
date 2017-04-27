package io.github.shogowada.scala.jsonrpc.serializers

trait JSONSerializer {
  def serialize[T](value: T): Option[String] = {
    throw new UnsupportedOperationException("This default implementation is here only to allow macros to be defined on child classes.")
  }

  def deserialize[T](json: String): Option[T] = {
    throw new UnsupportedOperationException("This default implementation is here only to allow macros to be defined on child classes.")
  }
}
