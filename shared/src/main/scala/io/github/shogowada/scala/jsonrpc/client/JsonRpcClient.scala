package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcClient[JSON_SERIALIZER <: JsonSerializer]
(
    val jsonSerializer: JSON_SERIALIZER,
    val jsonSender: JsonSender
) {
  val promisedResponseRepository = new JsonRpcPromisedResponseRepository

  def createApi[API]: API = macro JsonRpcClientMacro.createApi[API]

  def receive(json: String): Unit = macro JsonRpcClientMacro.receive
}

object JsonRpcClient {
  def apply[JSON_SERIALIZER <: JsonSerializer]
  (
      jsonSerializer: JSON_SERIALIZER,
      jsonSender: JsonSender
  ) = new JsonRpcClient(jsonSerializer, jsonSender)
}

object JsonRpcClientMacro {
  def createApi[API: c.WeakTypeTag]
  (c: blackbox.Context)
  : c.Expr[API] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    val memberFunctions = createMemberFunctions[c.type, API](c)
    c.Expr[API](
      q"""
          new {} with $apiType {
            ..$memberFunctions
          }
          """
    )
  }

  private def createMemberFunctions[CONTEXT <: blackbox.Context, API: c.WeakTypeTag]
  (c: CONTEXT)
  : Iterable[c.Tree] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    MacroUtils[c.type](c).getApiMethods(apiType)
        .map((apiMethod: MethodSymbol) => createMemberFunction[c.type](c)(apiMethod))
  }

  private def createMemberFunction[CONTEXT <: blackbox.Context]
  (c: CONTEXT)
  (apiMethod: c.universe.MethodSymbol)
  : c.Tree = {
    import c.universe._

    val name: TermName = apiMethod.name
    val parameterLists: List[List[Tree]] =
      apiMethod.paramLists.map((parameterList: List[Symbol]) => {
        parameterList.map((parameter: Symbol) => {
          q"${parameter.name.toTermName}: ${parameter.typeSignature}"
        })
      })
    val returnType: Type = apiMethod.returnType
    val tq"Future[$resultType]" = returnType

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"
    val jsonSender: Tree = q"${c.prefix.tree}.jsonSender"
    val promisedResponseRepository: Tree = q"${c.prefix.tree}.promisedResponseRepository"

    q"""
        override def $name(...$parameterLists): $returnType = {
          val requestId = Left(java.util.UUID.randomUUID.toString)
          val promisedResponse = $promisedResponseRepository.addAndGet(requestId)
          // Send request as JSON
          promisedResponse.future
              .map(json => {
                $jsonSerializer.deserialize[JsonRpcResultResponse[$resultType]](json)
                    .map(resultResponse => resultResponse.result)
              })
        }
        """
  }

  def receive
  (c: blackbox.Context)
  (json: c.Expr[String])
  : c.Expr[Unit] = {
    import c.universe._

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"
    val promisedResponseRepository: Tree = q"${c.prefix.tree}.promisedResponseRepository"

    val maybeJsonRpcResponse =
      q"""
          $jsonSerializer.deserialize[JsonRpcResponse]($json)
              .filter(response => response.jsonrpc == Constants.JsonRpc)
          """

    val maybePromisedResponse =
      q"""
          $maybeJsonRpcResponse
              .map(response => response.id)
              .flatMap(id => $promisedResponseRepository.getAndRemove(id))
          """

    c.Expr[Unit](
      q"""
          import io.github.shogowada.scala.jsonrpc.Constants
          import io.github.shogowada.scala.jsonrpc.Models._
          $maybePromisedResponse
              .foreach(promisedResponse => promisedResponse.success($json))
          """
    )
  }
}
