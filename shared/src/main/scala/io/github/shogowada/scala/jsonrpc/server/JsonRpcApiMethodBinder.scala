package io.github.shogowada.scala.jsonrpc.server

import java.lang.reflect.Method

class JsonRpcApiMethodBinder {

  def bind[API](server: JsonRpcServer, api: API, method: Method): Unit = {
  }
}

object JsonRpcApiMethodBinder {
  def apply() = new JsonRpcApiMethodBinder
}
