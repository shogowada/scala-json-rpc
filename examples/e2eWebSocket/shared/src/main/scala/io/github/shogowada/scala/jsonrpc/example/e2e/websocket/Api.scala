package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.Future

trait RandomNumberSubjectApi {
  def register(observerId: String): Unit

  def unregister(observerId: String): Unit
}

trait RandomNumberObserverApi {
  def getId: Future[String]

  def notify(randomNumber: Int): Unit
}
