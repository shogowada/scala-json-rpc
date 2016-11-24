package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Models.JsonRpcNotification

trait JsonRpcNotificationHandler {
  def handle(notification: JsonRpcNotification)(jsonRpcMethodRepository: JsonRpcMethodRepository): Unit
}
