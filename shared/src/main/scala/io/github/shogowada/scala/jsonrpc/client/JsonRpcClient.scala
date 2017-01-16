package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Types.{Id, JsonSender}
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcClient[JSON_SERIALIZER <: JsonSerializer]
(
    val jsonSerializer: JSON_SERIALIZER,
    val jsonSender: JsonSender,
    val executionContext: ExecutionContext
) {
  val promisedResponseRepository = new JsonRpcPromisedResponseRepository

  def send(json: String): Future[Option[String]] = jsonSender(json)

  def createApi[API]: API = macro JsonRpcClientMacro.createApi[API]

  def receive(json: String): Boolean = macro JsonRpcClientMacro.receive
}

object JsonRpcClient {
  def apply[JSON_SERIALIZER <: JsonSerializer](
      jsonSerializer: JSON_SERIALIZER,
      jsonSender: JsonSender
  )(implicit executionContext: ExecutionContext) = {
    new JsonRpcClient(
      jsonSerializer,
      jsonSender,
      executionContext
    )
  }
}

object JsonRpcClientMacro {
  def createApi[API: c.WeakTypeTag](c: blackbox.Context): c.Expr[API] = {
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
    MacroUtils[c.type](c).getJsonRpcApiMethods(apiType)
        .map((apiMethod: MethodSymbol) => createMemberFunction[c.type](c)(apiMethod))
  }

  private def createMemberFunction[CONTEXT <: blackbox.Context](c: CONTEXT)(
      apiMethod: c.universe.MethodSymbol
  ): c.Tree = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val paramTypes: Seq[Type] = apiMethod.paramLists.flatten
        .map(param => param.typeSignature)

    val function = macroUtils.createClientMethodAsFunction(
      c.prefix.tree,
      macroUtils.getJsonRpcMethodName(apiMethod),
      paramTypes,
      apiMethod.returnType
    )

    val name: TermName = apiMethod.name
    val parameterLists: List[List[Tree]] =
      apiMethod.paramLists.map((parameterList: List[Symbol]) => {
        parameterList.map((parameter: Symbol) => {
          q"${parameter.name.toTermName}: ${parameter.typeSignature}"
        })
      })
    val arguments: Seq[TermName] =
      apiMethod.paramLists.flatten
          .map(argument => argument.name.toTermName)

    q"""
        override def $name(...$parameterLists) = {
          $function(..$arguments)
        }
        """
  }

  def receive(c: blackbox.Context)(json: c.Expr[String]): c.Expr[Boolean] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val client = c.prefix.tree
    val jsonSerializer: Tree = q"$client.jsonSerializer"
    val promisedResponseRepository: Tree = q"$client.promisedResponseRepository"

    val maybeJsonRpcId = c.Expr[Option[Id]](
      q"""
          $jsonSerializer.deserialize[JsonRpcId]($json)
              .filter(requestId => requestId.jsonrpc == Constants.JsonRpc)
              .map(requestId => requestId.id)
          """
    )

    val maybePromisedResponse = c.Expr[Option[Promise[String]]](
      q"""
          $maybeJsonRpcId
              .flatMap(requestId => $promisedResponseRepository.getAndRemove(requestId))
          """
    )

    c.Expr[Boolean](
      q"""
          ..${macroUtils.imports}
          $maybePromisedResponse
              .map(promisedResponse => {
                promisedResponse.success($json)
                true
              })
              .getOrElse(false)
          """
    )
  }
}
