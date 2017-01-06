package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

class RandomNumberSubjectApiImpl extends RandomNumberSubjectApi {
  private var observerIds: Set[String] = Set()

  override def register(observerId: String): Unit = {
    this.synchronized {
      observerIds = observerIds + observerId
    }
  }

  override def unregister(observerId: String): Unit = {
    this.synchronized {
      observerIds = observerIds - observerId
    }
  }
}
