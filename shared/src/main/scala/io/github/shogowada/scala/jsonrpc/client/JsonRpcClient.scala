package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcClient[JSON_SERIALIZER <: JsonSerializer]
(
    val jsonSerializer: JSON_SERIALIZER
) {
  def createApi[API](api: API): API = macro JsonRpcClientMacro.createApi[API]
}

object JsonRpcClientMacro {
  def createApi[API: c.WeakTypeTag]
  (c: blackbox.Context)
  (api: c.Expr[API])
  : c.Expr[API] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    val apiMethods = MacroUtils[c.type](c).getApiMethods(apiType)
    q"""
        new $apiType {
        }
        """
    c.Expr[API](q"")
  }
}
