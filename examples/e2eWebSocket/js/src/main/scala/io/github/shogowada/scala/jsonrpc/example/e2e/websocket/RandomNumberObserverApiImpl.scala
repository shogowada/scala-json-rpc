package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.{Future, Promise}

class RandomNumberObserverApiImpl extends RandomNumberObserverApi {
  val promisedId: Promise[String] = Promise()

  override def getId: Future[String] = {
    promisedId.future
  }

  override def setId(id: String): Unit = {
    promisedId.success(id)
  }

  override def notify(randomNumber: Int): Unit = {
    println(randomNumber)
  }
}
