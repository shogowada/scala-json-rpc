package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.concurrent.Future
import scala.reflect.macros.blackbox

class DisposableFunctionClientFactoryMacro[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val methodClientFactoryMacro = new JSONRPCMethodClientFactoryMacro[c.type](c)

  def getOrCreate(
      server: c.Tree,
      client: c.Tree,
      disposableFunctionType: c.Type,
      disposableFunctionMethodName: c.Tree
  ): c.Tree = {
    val disposableFunctionRepository = macroUtils.getDisposableFunctionRepository(server)

    val newDisposableFunction = create(server, client, disposableFunctionType, disposableFunctionMethodName)

    q"""
        $disposableFunctionRepository
            .getOrAdd($disposableFunctionMethodName, () => $newDisposableFunction)
            .asInstanceOf[$disposableFunctionType]
        """
  }

  private def create(
      server: c.Tree,
      client: c.Tree,
      disposableFunctionType: c.Type,
      disposableFunctionMethodName: c.Tree
  ): c.Tree = {
    val typeArgs: Seq[Type] = disposableFunctionType.typeArgs
    val paramTypes: Seq[Type] = typeArgs.init
    val returnType: Type = typeArgs.last
    val function = methodClientFactoryMacro.createAsFunction(client, Some(server), disposableFunctionMethodName, paramTypes, returnType)

    val disposeMethod = createDisposeMethod(server, client, disposableFunctionMethodName)

    def getApplyParameterName(index: Int) = TermName(s"v$index")

    val applyParameters: Seq[Tree] = paramTypes.zipWithIndex
        .map { case (paramType, index) =>
          val paramName = getApplyParameterName(index)
          q"$paramName: $paramType"
        }
    val applyParameterNames = paramTypes.indices
        .map(getApplyParameterName)

    q"""
        new $disposableFunctionType {
          override val identifier = $function

          override def apply(..$applyParameters) = $function(..$applyParameterNames)

          $disposeMethod
        }
        """
  }

  private def createDisposeMethod(
      server: Tree,
      client: Tree,
      disposableFunctionMethodName: Tree
  ): Tree = {
    val disposableFunctionRepository = macroUtils.getDisposableFunctionRepository(server)

    val disposeClient = methodClientFactoryMacro.createAsFunction(
      client,
      Some(server),
      q"Constants.DisposeMethodName",
      Seq(macroUtils.getType[String]),
      macroUtils.getType[Future[Unit]]
    )

    q"""
        override def dispose(): Future[Unit] = {
          $disposableFunctionRepository.remove($disposableFunctionMethodName)
          $disposeClient($disposableFunctionMethodName)
        }
        """
  }
}
