package io.github.shogowada.scala.jsonrpc.common

import io.github.shogowada.scala.jsonrpc.DisposableFunction
import io.github.shogowada.scala.jsonrpc.Models.JSONRPCError
import io.github.shogowada.scala.jsonrpc.api.JSONRPCMethod

import scala.concurrent.Future
import scala.reflect.macros.blackbox

class JSONRPCMacroUtils[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val imports =
    q"""
        import scala.concurrent.Future
        import scala.util._
        import io.github.shogowada.scala.jsonrpc.Constants
        import io.github.shogowada.scala.jsonrpc.Models._
        import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer._
        """

  lazy val newUuid: c.Expr[String] = c.Expr[String](q"java.util.UUID.randomUUID.toString")

  def getJSONSerializer(prefix: Tree): Tree = q"$prefix.jsonSerializer"

  def getPromisedResponseRepository(prefix: Tree): Tree = q"$prefix.promisedResponseRepository"

  def getDisposableFunctionMethodNameRepository(prefix: Tree) = q"$prefix.disposableFunctionMethodNameRepository"

  def getRequestJSONHandlerRepository(prefix: Tree): Tree = q"$prefix.requestJSONHandlerRepository"

  def getDisposableFunctionRepository(prefix: Tree) = q"$prefix.disposableFunctionRepository"

  def getSend(prefix: Tree): Tree = q"$prefix.send"

  def getReceive(prefix: Tree): Tree = q"$prefix.receive"

  def getExecutionContext(prefix: Tree): Tree = q"$prefix.executionContext"

  def prefixDefinitionAndReference: (Tree, Tree) = {
    // We do this instead of using c.prefix.tree directly to make sure the reference
    // used at the point of macro expansion will always be used for the macro.
    // For example, if you have the following code
    //
    // server.bindAPI[API]
    // server = null
    //
    // then the API bound will break because its c.prefix.tree is now changed to null.
    val prefixTermName = TermName(c.freshName())
    (
        q"val $prefixTermName = ${c.prefix.tree}",
        q"$prefixTermName"
    )
  }

  def getJSONRPCAPIMethods(apiType: Type): Iterable[MethodSymbol] = {
    apiType.decls
        .filter((apiMember: Symbol) => isJSONRPCMethod(apiMember))
        .map((apiMember: Symbol) => apiMember.asMethod)
  }

  private def isJSONRPCMethod(method: Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  def getJSONRPCMethodName(method: MethodSymbol): String = {
    val maybeCustomMethodName: Option[String] = method.annotations
        .find(annotation => annotation.tree.tpe =:= typeOf[JSONRPCMethod])
        .map(annotation => annotation.tree.children.tail.head match {
          case Literal(Constant(name: String)) => name
        })
    maybeCustomMethodName.getOrElse(method.fullName)
  }

  def isDisposableFunctionType(theType: Type): Boolean = {
    theType <:< getType[DisposableFunction]
  }

  def isJSONRPCNotificationMethod(returnType: Type): Boolean = {
    returnType =:= getType[Unit]
  }

  def isJSONRPCRequestMethod(returnType: Type): Boolean = {
    returnType <:< getType[Future[_]]
  }

  def getType[T: c.TypeTag]: Type = {
    typeOf[T]
  }

  def createMaybeErrorJSONFromRequestJSON(
      serverOrClient: c.Tree,
      json: c.Expr[String],
      jsonRPCError: c.Expr[JSONRPCError[String]]
  ): c.Expr[Option[String]] = {
    val jsonSerializer: Tree = getJSONSerializer(serverOrClient)

    c.Expr[Option[String]](
      q"""
          $jsonSerializer.deserialize[JSONRPCId]($json)
              .map(requestId => requestId.id)
              .flatMap(id => ${createMaybeErrorJSONFromRequestId(serverOrClient, q"id", jsonRPCError)})
          """
    )
  }

  def createMaybeErrorJSONFromRequestId(
      serverOrClient: Tree,
      id: Tree,
      jsonRPCError: c.Expr[JSONRPCError[String]]
  ): c.Expr[Option[String]] = {
    val jsonSerializer: Tree = getJSONSerializer(serverOrClient)

    c.Expr[Option[String]](
      q"""
          $jsonSerializer.serialize(JSONRPCErrorResponse(
              jsonrpc = Constants.JSONRPC,
              id = $id,
              error = $jsonRPCError
          ))
          """
    )
  }
}

object JSONRPCMacroUtils {
  def apply[CONTEXT <: blackbox.Context](c: CONTEXT) = new JSONRPCMacroUtils[CONTEXT](c)
}
