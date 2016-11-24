package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Models.{JsonRpcRequest, JsonRpcResponse}

import scala.concurrent.Future

trait JsonRpcRequestHandler {
  def handle(request: JsonRpcRequest)(jsonRpcMethodRepository: JsonRpcMethodRepository): Future[JsonRpcResponse]
}
