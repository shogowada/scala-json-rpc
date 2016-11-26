package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.models.Models._

import scala.concurrent.Promise

class JsonRpcPromisedResponseRepository {

  private type ErrorOrResult = Either[JsonRpcErrorResponse, JsonRpcResponse]

  private var idToPromisedResponseMap: Map[Id, Promise[ErrorOrResult]] = Map()

  def addAndGet(id: Id): Promise[ErrorOrResult] = {
    this.synchronized {
      val promisedResponse: Promise[ErrorOrResult] = Promise()
      idToPromisedResponseMap = idToPromisedResponseMap + (id -> promisedResponse)
      promisedResponse
    }
  }

  def getAndRemove(id: Id): Option[Promise[ErrorOrResult]] = {
    this.synchronized {
      val maybePromisedResponse = idToPromisedResponseMap.get(id)
      idToPromisedResponseMap = idToPromisedResponseMap - id
      maybePromisedResponse
    }
  }
}
