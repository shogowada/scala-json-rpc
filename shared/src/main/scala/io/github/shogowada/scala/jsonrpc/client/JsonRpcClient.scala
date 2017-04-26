package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Types.{Id, JsonSender}
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcClient[JsonSerializerInUse <: JsonSerializer]
(
    val jsonSerializer: JsonSerializerInUse,
    val jsonSender: JsonSender,
    val executionContext: ExecutionContext
) {
  val promisedResponseRepository = new JsonRpcPromisedResponseRepository
  val disposableFunctionMethodNameRepository = new DisposableFunctionMethodNameRepository

  def send(json: String): Future[Option[String]] = jsonSender(json)

  def createAPI[API]: API = macro JsonRpcClientMacro.createAPI[API]

  def receive(json: String): Boolean = macro JsonRpcClientMacro.receive
}

object JsonRpcClient {
  def apply[JsonSerializerInUse <: JsonSerializer](
      jsonSerializer: JsonSerializerInUse,
      jsonSender: JsonSender
  )(implicit executionContext: ExecutionContext): JsonRpcClient[JsonSerializerInUse] = {
    new JsonRpcClient(
      jsonSerializer,
      jsonSender,
      executionContext
    )
  }
}

object JsonRpcClientMacro {
  def createAPI[API: c.WeakTypeTag](c: blackbox.Context): c.Expr[API] = {
    import c.universe._
    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val (clientDefinition, client) = macroUtils.prefixDefinitionAndReference
    val api = createApiImpl[c.type, API](c)(client, None)
    c.Expr[API](
      q"""
          $clientDefinition
          $api
          """
    )
  }

  def createApiImpl[Context <: blackbox.Context, API: c.WeakTypeTag](c: Context)(
      client: c.Tree,
      maybeServer: Option[c.Tree]
  ): c.Expr[API] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    val memberFunctions = createMemberFunctions[c.type, API](c)(client, maybeServer)
    c.Expr[API](
      q"""
          new {} with $apiType {
            ..$memberFunctions
          }
          """
    )
  }

  private def createMemberFunctions[Context <: blackbox.Context, API: c.WeakTypeTag](c: Context)(
      client: c.Tree,
      maybeServer: Option[c.Tree]
  ): Iterable[c.Tree] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    JsonRpcMacroUtils[c.type](c).getJsonRpcApiMethods(apiType)
        .map((apiMethod: MethodSymbol) => createMemberFunction[c.type](c)(client, maybeServer, apiMethod))
  }

  private def createMemberFunction[Context <: blackbox.Context](c: Context)(
      client: c.Tree,
      maybeServer: Option[c.Tree],
      apiMethod: c.universe.MethodSymbol
  ): c.Tree = {
    import c.universe._

    val macroUtils = JsonRpcMacroUtils[c.type](c)
    val methodClientFactoryMacro = new JsonRpcMethodClientFactoryMacro[c.type](c)

    val paramTypes: Seq[Type] = apiMethod.paramLists.flatten
        .map(param => param.typeSignature)

    val function = methodClientFactoryMacro.createAsFunction(
      client,
      maybeServer,
      q"${macroUtils.getJsonRpcMethodName(apiMethod)}",
      paramTypes,
      apiMethod.returnType
    )

    val name: TermName = apiMethod.name
    val parameterLists: List[List[Tree]] = apiMethod.paramLists
        .map((parameterList: List[Symbol]) => {
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

    val macroUtils = JsonRpcMacroUtils[c.type](c)

    val (clientDefinition, client) = macroUtils.prefixDefinitionAndReference
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
          $clientDefinition
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
