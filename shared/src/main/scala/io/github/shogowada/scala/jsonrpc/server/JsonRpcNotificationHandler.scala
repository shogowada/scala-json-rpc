package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Models.{JsonRpcNotification, JsonRpcNotificationMethod}

class JsonRpcNotificationHandler {
  def handle(notification: JsonRpcNotification, method: JsonRpcNotificationMethod): Unit = {
    method(notification)
  }
}
