package io.github.shogowada.scala.jsonrpc.client

import java.util.UUID

import io.github.shogowada.scala.jsonrpc.{Constants, JsonRpcFunction}

class JsonRpcFunctionMethodNameRepository {

  var jsonRpcFunctionToMethodNameMap: Map[Any, String] = Map()

  def getOrAddAndNotify(jsonRpcFunction: JsonRpcFunction[_], notify: (String) => Unit): String = this.synchronized {
    val function = jsonRpcFunction.function
    if (!jsonRpcFunctionToMethodNameMap.contains(function)) {
      val methodName = Constants.FunctionMethodNamePrefix + UUID.randomUUID().toString
      jsonRpcFunctionToMethodNameMap = jsonRpcFunctionToMethodNameMap + (function -> methodName)
      notify(methodName)
      methodName
    } else {
      jsonRpcFunctionToMethodNameMap(function)
    }
  }

  def dispose(jsonRpcFunction: JsonRpcFunction[_]): Unit = this.synchronized {
    jsonRpcFunctionToMethodNameMap = jsonRpcFunctionToMethodNameMap - jsonRpcFunction.function
  }
}
