package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.{Future, Promise}

class ClientApiImpl(promisedId: Promise[String]) extends ClientApi {
  def id: Future[String] = {
    promisedId.future
  }
}

class RandomNumberObserverApiImpl extends RandomNumberObserverApi {
  override def notify(randomNumber: Int): Unit = {
    println(randomNumber)
  }
}
