package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcErrorResponse, JsonRpcNotification, JsonRpcRequest, JsonRpcResultResponse}

import scala.concurrent.Future

object Types {
  type Id = Either[String, BigDecimal]

  type JsonRpcRequestMethod[PARAMS, ERROR, RESULT] = (JsonRpcRequest[PARAMS]) => Future[Either[JsonRpcErrorResponse[ERROR], JsonRpcResultResponse[RESULT]]]
  type JsonRpcNotificationMethod[PARAMS] = (JsonRpcNotification[PARAMS]) => Unit

  type JsonSender = (String) => Unit
}
