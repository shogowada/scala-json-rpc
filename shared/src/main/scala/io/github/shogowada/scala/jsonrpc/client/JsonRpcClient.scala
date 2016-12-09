package io.github.shogowada.scala.jsonrpc.client

import java.lang.reflect.{InvocationHandler, Proxy => JavaProxy}

import scala.reflect._

class JsonRpcClient {
  def createApi[API](api: API): API = {
    val classLoader = Thread.currentThread.getContextClassLoader
    val interfaces = Array[Class[_]](classTag[API].runtimeClass)
    val invocationHandler: InvocationHandler = _
    try {
      JavaProxy.newProxyInstance(classLoader, interfaces, invocationHandler).asInstanceOf[API]
    } catch {
      case exception: IllegalArgumentException =>
        throw new IllegalArgumentException("Failed to create an Scala JSON RPC API", exception)
    }
  }
}
