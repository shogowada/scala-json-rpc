package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Types._
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.{ExecutionContext, Future}

class JsonRpcClientBuilder[JSON_SERIALIZER <: JsonSerializer]
(
    jsonSerializer: JSON_SERIALIZER,
    jsonSender: (String) => Future[Option[String]]
) {
  def build: JsonRpcClient[JSON_SERIALIZER] = new JsonRpcClient(
    jsonSerializer,
    jsonSender
  )
}

object JsonRpcClientBuilder {
  def apply[JSON_SERIALIZER <: JsonSerializer]
  (
      jsonSerializer: JSON_SERIALIZER,
      jsonSender: (String) => Unit
  )(
      implicit executionContext: ExecutionContext
  ) = new JsonRpcClientBuilder(
    jsonSerializer,
    (json: String) => {
      jsonSender(json)
      Future(None)
    }
  )

  def apply[JSON_SERIALIZER <: JsonSerializer]
  (
      jsonSerializer: JSON_SERIALIZER,
      jsonSender: JsonSender
  ) = new JsonRpcClientBuilder(
    jsonSerializer,
    jsonSender
  )
}
