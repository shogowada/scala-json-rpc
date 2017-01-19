package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction

class JsonRpcFunctionRepository {
  var methodNameToJsonRpcFunctionMap: Map[String, JsonRpcFunction] = Map()

  def getOrAdd(methodName: String, jsonRpcFunctionFactory: () => JsonRpcFunction): JsonRpcFunction = this.synchronized {
    if (!methodNameToJsonRpcFunctionMap.contains(methodName)) {
      val jsonRpcFunction = jsonRpcFunctionFactory()
      methodNameToJsonRpcFunctionMap = methodNameToJsonRpcFunctionMap + (methodName -> jsonRpcFunction)
    }
    methodNameToJsonRpcFunctionMap(methodName)
  }

  def remove(methodName: String): Unit = this.synchronized {
    methodNameToJsonRpcFunctionMap = methodNameToJsonRpcFunctionMap - methodName
  }
}
