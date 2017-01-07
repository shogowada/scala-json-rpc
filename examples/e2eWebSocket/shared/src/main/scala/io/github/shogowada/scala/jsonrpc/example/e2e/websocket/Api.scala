package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.Future

trait RandomNumberSubjectApi {
  def register(): Future[String]

  def unregister(observerId: String): Unit
}

trait RandomNumberObserverApi {
  def notify(randomNumber: Int): Unit
}
