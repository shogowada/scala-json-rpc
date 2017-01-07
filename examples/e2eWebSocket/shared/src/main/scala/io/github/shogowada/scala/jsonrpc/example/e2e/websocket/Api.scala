package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.Future

trait RandomNumberSubjectApi {
  def register(observer: (Int) => Unit): Future[() => Unit]
}
