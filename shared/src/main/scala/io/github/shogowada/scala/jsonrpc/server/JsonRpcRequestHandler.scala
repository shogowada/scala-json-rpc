package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Models.{JsonRpcRequest, JsonRpcRequestMethod, JsonRpcResponse}

import scala.concurrent.Future

class JsonRpcRequestHandler {
  def handle(request: JsonRpcRequest, method: JsonRpcRequestMethod): Future[JsonRpcResponse] = {
    method(request)
  }
}
