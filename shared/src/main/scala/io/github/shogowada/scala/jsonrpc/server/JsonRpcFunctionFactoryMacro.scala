package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.client.JsonRpcMethodClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.concurrent.Future
import scala.reflect.macros.blackbox

class JsonRpcFunctionFactoryMacro[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val methodClientFactoryMacro = new JsonRpcMethodClientFactoryMacro[c.type](c)

  def getOrCreate(
      server: c.Tree,
      client: c.Tree,
      jsonRpcFunctionType: c.Type,
      jsonRpcFunctionMethodName: c.Tree
  ): c.Tree = {
    val jsonRpcFunctionRepository = macroUtils.getJsonRpcFunctionRepository(server)

    val newJsonRpcFunction = create(server, client, jsonRpcFunctionType, jsonRpcFunctionMethodName)

    q"""
        $jsonRpcFunctionRepository
            .getOrAdd($jsonRpcFunctionMethodName, () => $newJsonRpcFunction)
            .asInstanceOf[$jsonRpcFunctionType]
        """
  }

  private def create(
      server: c.Tree,
      client: c.Tree,
      jsonRpcFunctionType: c.Type,
      jsonRpcFunctionMethodName: c.Tree
  ): c.Tree = {
    val typeArgs: Seq[Type] = jsonRpcFunctionType.typeArgs
    val paramTypes: Seq[Type] = typeArgs.init
    val returnType: Type = typeArgs.last
    val function = methodClientFactoryMacro.createAsFunction(client, Some(server), jsonRpcFunctionMethodName, paramTypes, returnType)

    val disposeMethod = createDisposeMethod(server, client, jsonRpcFunctionMethodName)

    def getApplyParameterName(index: Int) = TermName(s"v$index")

    val applyParameters: Seq[Tree] = paramTypes.zipWithIndex
        .map { case (paramType, index) =>
          val paramName = getApplyParameterName(index)
          q"$paramName: $paramType"
        }
    val applyParameterNames = paramTypes.indices
        .map(getApplyParameterName)

    q"""
        new $jsonRpcFunctionType {
          override val identifier = $function

          override def apply(..$applyParameters) = $function(..$applyParameterNames)

          $disposeMethod
        }
        """
  }

  private def createDisposeMethod(
      server: Tree,
      client: Tree,
      jsonRpcFunctionMethodName: Tree
  ): Tree = {
    val jsonRpcFunctionRepository = macroUtils.getJsonRpcFunctionRepository(server)

    val disposeClient = methodClientFactoryMacro.createAsFunction(
      client,
      Some(server),
      q"Constants.DisposeMethodName",
      Seq(macroUtils.getType[String]),
      macroUtils.getType[Future[Unit]]
    )

    q"""
        override def dispose(): Future[Unit] = {
          $jsonRpcFunctionRepository.remove($jsonRpcFunctionMethodName)
          $disposeClient($jsonRpcFunctionMethodName)
        }
        """
  }
}
