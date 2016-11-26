package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.models.Models._

import scala.concurrent.Promise

class JsonRpcPromisedResponseRepository {

  private var idToPromisedResponseMap: Map[Id, Promise[JsonRpcResponse]] = Map()

  def addAndGet(id: Id): Promise[JsonRpcResponse] = {
    this.synchronized {
      val promisedResponse: Promise[JsonRpcResponse] = Promise()
      idToPromisedResponseMap = idToPromisedResponseMap + (id -> promisedResponse)
      promisedResponse
    }
  }

  def getAndRemove(id: Id): Option[Promise[JsonRpcResponse]] = {
    this.synchronized {
      val maybePromisedResponse = idToPromisedResponseMap.get(id)
      idToPromisedResponseMap = idToPromisedResponseMap - id
      maybePromisedResponse
    }
  }
}
