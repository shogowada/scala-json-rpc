package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer.RequestJSONHandler

class JSONRPCRequestJSONHandlerRepository {
  private var methodNameToHandlerMap: Map[String, RequestJSONHandler] = Map.empty

  def add(methodName: String, requestJSONHandler: RequestJSONHandler): Unit = this.synchronized {
    methodNameToHandlerMap = methodNameToHandlerMap + (methodName -> requestJSONHandler)
  }

  def addIfAbsent(methodName: String, handlerFactory: () => (RequestJSONHandler)): Unit = this.synchronized {
    if (!methodNameToHandlerMap.contains(methodName)) {
      add(methodName, handlerFactory())
    }
  }

  def get(methodName: String): Option[RequestJSONHandler] = {
    methodNameToHandlerMap.get(methodName)
  }

  def remove(methodName: String): Unit = this.synchronized {
    methodNameToHandlerMap = methodNameToHandlerMap - methodName
  }
}
