package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.shared.Receiver

trait JsonRpcClient extends Receiver {
  def createApiClient[T]: T
}
