package io.github.shogowada.scala.jsonrpc.client

import java.util.UUID

import io.github.shogowada.scala.jsonrpc.{Constants, JsonRpcFunction}

class JsonRpcFunctionMethodNameRepository {

  var jsonRpcFunctionToMethodNameMap: Map[Any, String] = Map()
  var jsonRpcMethodNameToFunctionMap: Map[String, Any] = Map()

  def getOrAddAndNotify(jsonRpcFunction: JsonRpcFunction[_], notify: (String) => Unit): String = this.synchronized {
    val original = jsonRpcFunction.original
    if (!jsonRpcFunctionToMethodNameMap.contains(original)) {
      val methodName = Constants.FunctionMethodNamePrefix + UUID.randomUUID().toString
      jsonRpcFunctionToMethodNameMap = jsonRpcFunctionToMethodNameMap + (original -> methodName)
      jsonRpcMethodNameToFunctionMap = jsonRpcMethodNameToFunctionMap + (methodName -> original)
      notify(methodName)
      methodName
    } else {
      jsonRpcFunctionToMethodNameMap(original)
    }
  }

  def remove(methodName: String): Unit = this.synchronized {
    jsonRpcMethodNameToFunctionMap.get(methodName)
        .foreach(function => {
          jsonRpcFunctionToMethodNameMap = jsonRpcFunctionToMethodNameMap - function
          jsonRpcMethodNameToFunctionMap = jsonRpcMethodNameToFunctionMap - methodName
        })
  }
}
