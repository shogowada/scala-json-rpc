package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.Future
import scala.language.higherKinds

trait JsonReceiver {
  def receive[SERIALIZER[_], DESERIALIZER[_]]
  (
      json: String
  )(
      jsonSerializer: JsonSerializer[SERIALIZER, DESERIALIZER]
  ): Future[Option[String]]
}
