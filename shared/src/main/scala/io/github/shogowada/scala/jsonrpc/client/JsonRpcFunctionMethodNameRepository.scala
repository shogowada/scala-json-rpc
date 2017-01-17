package io.github.shogowada.scala.jsonrpc.client

import java.util.UUID

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction

class JsonRpcFunctionMethodNameRepository {

  var jsonRpcFunctionToMethodNameMap: Map[JsonRpcFunction[_], String] = Map()

  def getOrAdd(jsonRpcFunction: JsonRpcFunction[_]): String = this.synchronized {
    if (!jsonRpcFunctionToMethodNameMap.contains(jsonRpcFunction)) {
      val methodName = UUID.randomUUID().toString
      jsonRpcFunctionToMethodNameMap = jsonRpcFunctionToMethodNameMap + (jsonRpcFunction -> methodName)
    }
    jsonRpcFunctionToMethodNameMap(jsonRpcFunction)
  }

  def dispose(jsonRpcFunction: JsonRpcFunction[_]): Unit = this.synchronized {
    jsonRpcFunctionToMethodNameMap = jsonRpcFunctionToMethodNameMap - jsonRpcFunction
  }
}
