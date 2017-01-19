package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.RequestJsonHandler

class JsonRpcRequestJsonHandlerRepository {
  var methodNameToHandlerMap: Map[String, RequestJsonHandler] = Map()

  def add(methodName: String, requestJsonHandler: RequestJsonHandler): Unit = this.synchronized {
    methodNameToHandlerMap = methodNameToHandlerMap + (methodName -> requestJsonHandler)
  }

  def addIfAbsent(methodName: String, handlerFactory: () => (RequestJsonHandler)): Unit = this.synchronized {
    if (!methodNameToHandlerMap.contains(methodName)) {
      add(methodName, handlerFactory())
    }
  }

  def get(methodName: String): Option[RequestJsonHandler] = {
    methodNameToHandlerMap.get(methodName)
  }

  def remove(methodName: String): Unit = this.synchronized {
    methodNameToHandlerMap = methodNameToHandlerMap - methodName
  }
}
