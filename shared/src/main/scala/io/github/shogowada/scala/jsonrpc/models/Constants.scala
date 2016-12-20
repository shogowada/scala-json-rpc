package io.github.shogowada.scala.jsonrpc.models

import scala.concurrent.Future

object Constants {
  val JsonRpc = "2.0"
}

object Types {
  type Id = Either[String, BigDecimal]

  type JsonRpcRequestMethod[PARAMS, ERROR, RESULT] = (JsonRpcRequest[PARAMS]) => Future[Either[JsonRpcErrorResponse[ERROR], JsonRpcResponse[RESULT]]]
  type JsonRpcNotificationMethod[PARAMS] = (JsonRpcNotification[PARAMS]) => Unit
}
