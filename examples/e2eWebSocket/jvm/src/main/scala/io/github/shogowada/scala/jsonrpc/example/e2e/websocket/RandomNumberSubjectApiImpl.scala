package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.Future

class RandomNumberSubjectApiImpl extends RandomNumberSubjectApi {
  private var observers: Set[(Int) => Unit] = Set()

  override def register(observer: (Int) => Unit): Future[() => Unit] = {
    this.synchronized(observers = observers + observer)
    Future(() => {
      this.synchronized(observers = observers - observer)
    })
  }
}
