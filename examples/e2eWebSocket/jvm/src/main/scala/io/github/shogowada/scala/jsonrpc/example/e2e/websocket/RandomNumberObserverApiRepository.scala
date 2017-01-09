package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

class RandomNumberObserverApiRepository {
  private var idToApiMap: Map[String, RandomNumberObserverApi] = Map()

  def add(clientId: String, api: RandomNumberObserverApi): Unit = {
    this.synchronized(idToApiMap = idToApiMap + (clientId -> api))
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
