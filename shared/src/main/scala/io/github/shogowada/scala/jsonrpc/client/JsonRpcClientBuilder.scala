package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Types._
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros

class JsonRpcClientBuilder[JSON_SERIALIZER <: JsonSerializer](
    jsonSerializer: JSON_SERIALIZER,
    jsonSender: JsonSender,
    executionContext: ExecutionContext
) {
  def build: JsonRpcClient[JSON_SERIALIZER] = new JsonRpcClient(
    jsonSerializer,
    jsonSender,
    executionContext
  )
}

object JsonRpcClientBuilder {
  def apply[JSON_SERIALIZER <: JsonSerializer](
      jsonSerializer: JSON_SERIALIZER,
      jsonSender: (String) => Future[Option[String]]
  )(
      implicit executionContext: ExecutionContext
  ): JsonRpcClientBuilder[JSON_SERIALIZER] = {
    new JsonRpcClientBuilder(
      jsonSerializer,
      jsonSender,
      executionContext
    )
  }
}
