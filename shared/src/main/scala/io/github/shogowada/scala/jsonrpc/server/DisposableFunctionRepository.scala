package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.DisposableFunction

class DisposableFunctionRepository {
  private var methodNameToDisposableFunctionMap: Map[String, DisposableFunction] = Map.empty

  def getOrAdd(methodName: String, disposableFunctionFactory: () => DisposableFunction): DisposableFunction = this.synchronized {
    if (!methodNameToDisposableFunctionMap.contains(methodName)) {
      val disposableFunction = disposableFunctionFactory()
      methodNameToDisposableFunctionMap = methodNameToDisposableFunctionMap + (methodName -> disposableFunction)
    }
    methodNameToDisposableFunctionMap(methodName)
  }

  def remove(methodName: String): Unit = this.synchronized {
    methodNameToDisposableFunctionMap = methodNameToDisposableFunctionMap - methodName
  }
}
