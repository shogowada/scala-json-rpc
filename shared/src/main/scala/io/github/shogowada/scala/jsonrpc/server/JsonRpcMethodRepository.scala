package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.Models.{JsonRpcNotificationMethod, JsonRpcRequestMethod}

class JsonRpcMethodRepository {

  private var nameToRequestMethodMap: Map[String, JsonRpcRequestMethod] = Map()
  private var nameToNotificationMethodMap: Map[String, JsonRpcNotificationMethod] = Map()

  def bind(name: String, method: JsonRpcRequestMethod): Unit = {
    this.synchronized {
      nameToRequestMethodMap = nameToRequestMethodMap + (name -> method)
    }
  }

  def bind(name: String, method: JsonRpcNotificationMethod): Unit = {
    this.synchronized {
      nameToNotificationMethodMap = nameToNotificationMethodMap + (name -> method)
    }
  }

  def getRequestMethod(method: String): Option[JsonRpcRequestMethod] = {
    nameToRequestMethodMap.get(method)
  }

  def getNotificationMethod(method: String): Option[JsonRpcNotificationMethod] = {
    nameToNotificationMethodMap.get(method)
  }
}
