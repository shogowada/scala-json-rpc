package io.github.shogowada.scala.jsonrpc.utils

import io.github.shogowada.scala.jsonrpc.DisposableFunction
import io.github.shogowada.scala.jsonrpc.Models.JsonRpcError
import io.github.shogowada.scala.jsonrpc.api.JsonRpcMethod

import scala.concurrent.Future
import scala.reflect.macros.blackbox

class JsonRpcMacroUtils[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val imports =
    q"""
        import scala.concurrent.Future
        import scala.util._
        import io.github.shogowada.scala.jsonrpc.Constants
        import io.github.shogowada.scala.jsonrpc.Models._
        import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer._
        """

  lazy val newUuid: c.Expr[String] = c.Expr[String](q"java.util.UUID.randomUUID.toString")

  def getJsonSerializer(prefix: Tree): Tree = q"$prefix.jsonSerializer"

  def getPromisedResponseRepository(prefix: Tree): Tree = q"$prefix.promisedResponseRepository"

  def getDisposableFunctionMethodNameRepository(prefix: Tree) = q"$prefix.disposableFunctionMethodNameRepository"

  def getRequestJsonHandlerRepository(prefix: Tree): Tree = q"$prefix.requestJsonHandlerRepository"

  def getDisposableFunctionRepository(prefix: Tree) = q"$prefix.disposableFunctionRepository"

  def getSend(prefix: Tree): Tree = q"$prefix.send"

  def getReceive(prefix: Tree): Tree = q"$prefix.receive"

  def getExecutionContext(prefix: Tree): Tree = q"$prefix.executionContext"

  def prefixDefinitionAndReference: (Tree, Tree) = {
    // We do this instead of using c.prefix.tree directly to make sure the reference
    // used at the point of macro expansion will always be used for the macro.
    // For example, if you have the following code
    //
    // server.bindApi[Api]
    // server = null
    //
    // then the API bound will break because its c.prefix.tree is now changed to null.
    val prefixTermName = TermName(c.freshName())
    (
        q"val $prefixTermName = ${c.prefix.tree}",
        q"$prefixTermName"
    )
  }

  def getJsonRpcApiMethods(apiType: Type): Iterable[MethodSymbol] = {
    apiType.decls
        .filter((apiMember: Symbol) => isJsonRpcMethod(apiMember))
        .map((apiMember: Symbol) => apiMember.asMethod)
  }

  private def isJsonRpcMethod(method: Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  def getJsonRpcMethodName(method: MethodSymbol): String = {
    val maybeCustomMethodName: Option[String] = method.annotations
        .find(annotation => annotation.tree.tpe =:= typeOf[JsonRpcMethod])
        .map(annotation => annotation.tree.children.tail.head match {
          case Literal(Constant(name: String)) => name
        })
    maybeCustomMethodName.getOrElse(method.fullName)
  }

  def getJsonRpcParameterType(paramTypes: Seq[c.Type]): Tree = {
    val parameterTypes: Iterable[Type] = paramTypes
        .map(mapSingleJsonRpcParameterType)

    if (parameterTypes.size == 1) {
      val parameterType = parameterTypes.head
      tq"Tuple1[$parameterType]"
    } else {
      tq"(..$parameterTypes)"
    }
  }

  private def mapSingleJsonRpcParameterType(paramType: Type): Type = {
    if (isDisposableFunctionType(paramType)) {
      getType[String]
    } else {
      paramType
    }
  }

  def getJsonRpcResultType(resultType: Type): Type = {
    if (isDisposableFunctionType(resultType)) {
      getType[String]
    } else {
      resultType
    }
  }

  def isDisposableFunctionType(theType: Type): Boolean = {
    theType <:< getType[DisposableFunction]
  }

  def isJsonRpcNotificationMethod(returnType: Type): Boolean = {
    returnType =:= getType[Unit]
  }

  def isJsonRpcRequestMethod(returnType: Type): Boolean = {
    returnType <:< getType[Future[_]]
  }

  def getType[T: c.TypeTag]: Type = {
    typeOf[T]
  }

  def createMaybeErrorJsonFromRequestJson(
      serverOrClient: c.Tree,
      json: c.Expr[String],
      jsonRpcError: c.Expr[JsonRpcError[String]]
  ): c.Expr[Option[String]] = {
    val jsonSerializer: Tree = getJsonSerializer(serverOrClient)

    c.Expr[Option[String]](
      q"""
          $jsonSerializer.deserialize[JsonRpcId]($json)
              .map(requestId => requestId.id)
              .flatMap(id => ${createMaybeErrorJsonFromRequestId(serverOrClient, q"id", jsonRpcError)})
          """
    )
  }

  def createMaybeErrorJsonFromRequestId(
      serverOrClient: Tree,
      id: Tree,
      jsonRpcError: c.Expr[JsonRpcError[String]]
  ): c.Expr[Option[String]] = {
    val jsonSerializer: Tree = getJsonSerializer(serverOrClient)

    c.Expr[Option[String]](
      q"""
          $jsonSerializer.serialize(JsonRpcErrorResponse(
              jsonrpc = Constants.JsonRpc,
              id = $id,
              error = $jsonRpcError
          ))
          """
    )
  }
}

object JsonRpcMacroUtils {
  def apply[CONTEXT <: blackbox.Context](c: CONTEXT) = new JsonRpcMacroUtils[CONTEXT](c)
}
