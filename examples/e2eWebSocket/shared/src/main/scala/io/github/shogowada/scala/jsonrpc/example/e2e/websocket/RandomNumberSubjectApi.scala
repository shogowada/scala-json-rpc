package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction1

import scala.concurrent.Future

trait RandomNumberSubjectApi {
  def register(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit

  def unregister(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit
}
