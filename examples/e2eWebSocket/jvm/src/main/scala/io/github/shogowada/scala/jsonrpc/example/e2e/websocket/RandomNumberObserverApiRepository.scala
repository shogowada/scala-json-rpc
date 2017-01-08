package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class RandomNumberObserverApiRepository {
  private var idToApiMap: Map[String, RandomNumberObserverApi] = Map()

  def add(api: RandomNumberObserverApi): Unit = {
    api.getId.onComplete {
      case Success(id) => this.synchronized(idToApiMap = idToApiMap + (id -> api))
      case _ =>
    }
  }

  def remove(apiToRemove: RandomNumberObserverApi): Option[String] = {
    this.synchronized {
      val maybeId: Option[String] = idToApiMap
          .find {
            case (_, api) if api == apiToRemove => true
            case _ => false
          }
          .map {
            case (id, _) => id
          }
      maybeId.foreach(id => idToApiMap = idToApiMap - id)
      maybeId
    }
  }

  def getIdToApiMap: Map[String, RandomNumberObserverApi] = {
    idToApiMap
  }
}
