package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.Future

// Server side
trait ClientIdFactoryApi {
  def create(): Future[String]
}

trait RandomNumberSubjectApi {
  def register(clientId: String): Unit

  def unregister(clientId: String): Unit
}

// Client side
trait ClientApi {
  def id: Future[String]
}

trait RandomNumberObserverApi {
  def notify(randomNumber: Int): Unit
}
