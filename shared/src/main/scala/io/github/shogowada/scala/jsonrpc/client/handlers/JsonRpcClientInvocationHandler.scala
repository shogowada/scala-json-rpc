package io.github.shogowada.scala.jsonrpc.client.handlers

import java.lang.reflect.{InvocationHandler, Method}

class JsonRpcClientInvocationHandler extends InvocationHandler {
  override def invoke(proxy: Any, method: Method, args: Array[AnyRef]): AnyRef = {
    val methodName = method.getName
    val parameters = method.getParameters
    throw new UnsupportedOperationException
  }
}
