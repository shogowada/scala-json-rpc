package io.github.shogowada.scala.jsonrpc.utils

import scala.reflect.macros.blackbox

class MacroUtils[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  def getApiMethods
  (apiType: Type)
  : Iterable[MethodSymbol] = {
    apiType.decls
        .filter((apiMember: Symbol) => isJsonRpcMethod(apiMember))
        .map((apiMember: Symbol) => apiMember.asMethod)
  }

  private def isJsonRpcMethod(method: Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  def getMethodName(method: MethodSymbol): String = {
    method.fullName
  }

  def getParameterType(method: MethodSymbol): Tree = {
    val parameterTypes: Iterable[Type] = method.asMethod.paramLists
        .flatMap((paramList: List[Symbol]) => paramList)
        .map((param: Symbol) => param.typeSignature)

    if (parameterTypes.size == 1) {
      val parameterType = parameterTypes.head
      tq"Tuple1[$parameterType]"
    } else {
      tq"(..$parameterTypes)"
    }
  }

  def isNotificationMethod(method: MethodSymbol): Boolean = {
    val returnType: Type = method.returnType
    returnType =:= getType[Unit]
  }

  def getType[T: c.TypeTag]: Type = {
    typeOf[T]
  }
}

object MacroUtils {
  def apply[CONTEXT <: blackbox.Context](c: CONTEXT) = new MacroUtils[CONTEXT](c)
}
