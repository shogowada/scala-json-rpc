package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.{Future, Promise}

class RandomNumberObserverApiImpl(promisedId: Promise[String]) extends RandomNumberObserverApi {
  override def getId: Future[String] = {
    promisedId.future
  }

  override def notify(randomNumber: Int): Unit = {
    println(randomNumber)
  }
}
