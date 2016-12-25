package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Types.Id

import scala.concurrent.Promise

class JsonRpcPromisedResponseRepository {

  private var idToPromisedResponseMap: Map[Id, Promise[String]] = Map()

  def addAndGet(id: Id): Promise[String] = {
    this.synchronized {
      val promisedResponse: Promise[String] = Promise()
      idToPromisedResponseMap = idToPromisedResponseMap + (id -> promisedResponse)
      promisedResponse
    }
  }

  def getAndRemove(id: Id): Option[Promise[String]] = {
    this.synchronized {
      val maybePromisedResponse = idToPromisedResponseMap.get(id)
      idToPromisedResponseMap = idToPromisedResponseMap - id
      maybePromisedResponse
    }
  }
}
