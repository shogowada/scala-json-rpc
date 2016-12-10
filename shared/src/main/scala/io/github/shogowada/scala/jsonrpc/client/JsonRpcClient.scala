package io.github.shogowada.scala.jsonrpc.client

import java.lang.reflect.{InvocationHandler, Proxy => JavaProxy}

import io.github.shogowada.scala.jsonrpc.client.handlers.JsonRpcClientInvocationHandler

import scala.reflect._

class JsonRpcClient {
  def createApi[API: ClassTag](api: API): API = {
    val classLoader = Thread.currentThread.getContextClassLoader
    val interfaces = Array[Class[_]](classTag[API].runtimeClass)
    val invocationHandler: InvocationHandler = new JsonRpcClientInvocationHandler
    try {
      JavaProxy.newProxyInstance(classLoader, interfaces, invocationHandler).asInstanceOf[API]
    } catch {
      case exception: IllegalArgumentException =>
        throw new IllegalArgumentException("Failed to create an Scala JSON RPC API", exception)
    }
  }
}
