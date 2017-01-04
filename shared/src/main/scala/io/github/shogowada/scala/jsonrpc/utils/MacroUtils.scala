package io.github.shogowada.scala.jsonrpc.utils

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcError, JsonRpcErrorResponse}
import io.github.shogowada.scala.jsonrpc.api.JsonRpcMethod

import scala.reflect.macros.blackbox

class MacroUtils[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  val imports =
    q"""
        import scala.concurrent.Future
        import scala.util._
        import io.github.shogowada.scala.jsonrpc.Constants
        import io.github.shogowada.scala.jsonrpc.Models._
        import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer._
        """

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
    val maybeCustomMethodName: Option[String] = method.annotations
        .find(annotation => annotation.tree.tpe =:= typeOf[JsonRpcMethod])
        .map(annotation => annotation.tree.children.tail.head match {
          case Literal(Constant(name: String)) => name
        })
    maybeCustomMethodName.getOrElse(method.fullName)
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

  def createMaybeErrorJson
  (json: c.Expr[String], jsonRpcError: c.Expr[JsonRpcError[String]])
  : c.Expr[Option[String]] = {
    import c.universe._

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"

    val error = (id: TermName) => c.Expr[JsonRpcErrorResponse[String]](
      q"""
          JsonRpcErrorResponse(
            jsonrpc = Constants.JsonRpc,
            id = $id,
            error = $jsonRpcError
          )
          """
    )

    val maybeErrorJson = (id: TermName) => c.Expr[Option[String]](
      q"""$jsonSerializer.serialize(${error(id)})"""
    )

    c.Expr[Option[String]](
      q"""
          $jsonSerializer.deserialize[JsonRpcId]($json)
            .map(requestId => requestId.id)
            .flatMap(id => {
              ${maybeErrorJson(TermName("id"))}
            })
          """
    )
  }
}

object MacroUtils {
  def apply[CONTEXT <: blackbox.Context](c: CONTEXT) = new MacroUtils[CONTEXT](c)
}
