package io.github.shogowada.scala.jsonrpc.client

import java.util.UUID

import io.github.shogowada.scala.jsonrpc.{Constants, JsonRpcFunction}

class JsonRpcFunctionMethodNameRepository {

  var identifierToMethodNameMap: Map[Any, String] = Map()
  var methodNameToIdentifierMap: Map[String, Any] = Map()

  def getOrAddAndNotify(jsonRpcFunction: JsonRpcFunction, notify: (String) => Unit): String = this.synchronized {
    val identifier = jsonRpcFunction.identifier
    if (!identifierToMethodNameMap.contains(identifier)) {
      val methodName = Constants.FunctionMethodNamePrefix + UUID.randomUUID().toString
      identifierToMethodNameMap = identifierToMethodNameMap + (identifier -> methodName)
      methodNameToIdentifierMap = methodNameToIdentifierMap + (methodName -> identifier)
      notify(methodName)
      methodName
    } else {
      identifierToMethodNameMap(identifier)
    }
  }

  def remove(methodName: String): Unit = this.synchronized {
    methodNameToIdentifierMap.get(methodName)
        .foreach(identifier => {
          identifierToMethodNameMap = identifierToMethodNameMap - identifier
          methodNameToIdentifierMap = methodNameToIdentifierMap - methodName
        })
  }
}
