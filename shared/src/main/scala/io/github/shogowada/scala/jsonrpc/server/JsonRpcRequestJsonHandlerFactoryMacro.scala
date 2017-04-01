package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcError
import io.github.shogowada.scala.jsonrpc.client.DisposableFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.RequestJsonHandler
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcRequestJsonHandlerFactoryMacro[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val disposableFunctionClientFactoryMacro = new DisposableFunctionClientFactoryMacro[c.type](c)
  lazy val disposableFunctionServerFactoryMacro = new DisposableFunctionServerFactoryMacro[c.type](c)

  case class RequestJsonHandlerContext(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      methodName: c.Tree,
      parameterTypeLists: Seq[Seq[c.Type]],
      returnType: c.Type
  )

  def createFromApiMethod[API](
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[RequestJsonHandler] = {
    val parameterTypeLists: List[List[Type]] = method.asMethod.paramLists
        .map(parameters => parameters.map(parameter => parameter.typeSignature))

    create(RequestJsonHandlerContext(
      server = server,
      maybeClient = maybeClient,
      methodName = q"$api.$method",
      parameterTypeLists = parameterTypeLists,
      returnType = method.asMethod.returnType
    ))
  }

  def createFromDisposableFunction(
      client: Tree,
      server: Tree,
      disposableFunction: TermName,
      disposableFunctionType: Type
  ): c.Expr[RequestJsonHandler] = {
    val paramTypes: Seq[Type] = disposableFunctionType.typeArgs.init
    val returnType: Type = disposableFunctionType.typeArgs.last

    create(RequestJsonHandlerContext(
      server = server,
      maybeClient = Some(client),
      methodName = q"$disposableFunction",
      parameterTypeLists = Seq(paramTypes),
      returnType = returnType
    ))
  }

  private def create(handlerContext: RequestJsonHandlerContext): c.Expr[RequestJsonHandler] = {
    if (macroUtils.isJsonRpcNotificationMethod(handlerContext.returnType)) {
      createNotificationHandler(handlerContext)
    } else {
      createRequestHandler(handlerContext)
    }
  }

  private def createNotificationHandler(handlerContext: RequestJsonHandlerContext): c.Expr[RequestJsonHandler] = {
    val jsonSerializer = macroUtils.getJsonSerializer(handlerContext.server)
    val executionContext = macroUtils.getExecutionContext(handlerContext.server)

    val params = TermName("params")

    val jsonRpcParameterType: Tree = macroUtils.getJsonRpcParameterType(handlerContext.parameterTypeLists.flatten)

    def methodInvocation(params: TermName) = createMethodInvocation(handlerContext, params)

    c.Expr[RequestJsonHandler](
      q"""
          (json: String) => {
            ..${macroUtils.imports}
            $jsonSerializer.deserialize[JsonRpcNotification[$jsonRpcParameterType]](json)
              .foreach(notification => {
                val $params = notification.params
                ${methodInvocation(params)}
              })
            Future(None)($executionContext)
          }
          """
    )
  }

  private def createRequestHandler(handlerContext: RequestJsonHandlerContext): c.Expr[RequestJsonHandler] = {
    val jsonSerializer = macroUtils.getJsonSerializer(handlerContext.server)
    val executionContext = macroUtils.getExecutionContext(handlerContext.server)

    val json = TermName("json")
    val request = TermName("request")
    val params = TermName("params")

    def maybeInvalidParamsErrorJson(json: TermName): c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJsonFromRequestJson(
        handlerContext.server,
        c.Expr[String](q"$json"),
        c.Expr[JsonRpcError[String]](q"JsonRpcErrors.invalidParams")
      )

    val jsonRpcParameterType: Tree = macroUtils.getJsonRpcParameterType(handlerContext.parameterTypeLists.flatten)

    def methodInvocation(params: TermName): Tree = createMethodInvocation(handlerContext, params)

    c.Expr[RequestJsonHandler](
      q"""
          ($json: String) => {
            ..${macroUtils.imports}
            $jsonSerializer.deserialize[JsonRpcRequest[$jsonRpcParameterType]]($json)
              .map(($request: JsonRpcRequest[$jsonRpcParameterType]) => {
                val $params = $request.params
                ${methodInvocation(params)}
                  .map((result) => JsonRpcResultResponse(
                    jsonrpc = Constants.JsonRpc,
                    id = $request.id,
                    result = result
                  ))($executionContext)
                  .map((response) => $jsonSerializer.serialize(response))($executionContext)
              })
              .getOrElse(Future(${maybeInvalidParamsErrorJson(json)})($executionContext))
          }
          """
    )
  }

  private def createMethodInvocation(
      handlerContext: RequestJsonHandlerContext,
      params: TermName
  ): Tree = {
    val executionContext = macroUtils.getExecutionContext(handlerContext.server)

    val realMethodInvocation = if (handlerContext.parameterTypeLists.isEmpty) {
      q"${handlerContext.methodName}"
    } else {
      q"${handlerContext.methodName}(..${createArguments(handlerContext, params)})"
    }

    val returnsDisposableFunction = handlerContext.returnType.typeArgs.headOption
        .exists(macroUtils.isDisposableFunctionType)

    if (returnsDisposableFunction) {
      def disposableFunctionServer(disposableFunction: TermName): c.Expr[String] =
        handlerContext.maybeClient
            .map(client => disposableFunctionServerFactoryMacro.getOrCreate(client, handlerContext.server, disposableFunction, handlerContext.returnType))
            .getOrElse(throw new UnsupportedOperationException("To return DisposableFunction, you need to bind the API to JsonRpcServerAndClient."))
      q"""
          $realMethodInvocation
              .map(disposableFunction => ${disposableFunctionServer(TermName("disposableFunction"))})($executionContext)
          """
    } else {
      realMethodInvocation
    }
  }

  private def createArguments(
      handlerContext: RequestJsonHandlerContext,
      params: TermName
  ): Seq[Tree] = {
    val parameterTypes: Seq[Type] = handlerContext.parameterTypeLists.flatten

    def createArgument(parameterType: Type, index: Int): Tree = {
      val fieldName = TermName(s"_${index + 1}")
      val argument = q"$params.$fieldName"
      if (macroUtils.isDisposableFunctionType(parameterType)) {
        handlerContext.maybeClient
            .map(client => disposableFunctionClientFactoryMacro.getOrCreate(handlerContext.server, client, parameterType, argument))
            .getOrElse(throw new UnsupportedOperationException("To use DisposableFunction, you need to bind the API to JsonRpcServerAndClient."))
      } else {
        argument
      }
    }

    parameterTypes
        .zipWithIndex
        .map { case (parameterType, index) => createArgument(parameterType, index) }
  }

  def createDisposeFunctionMethodHandler(
      server: Tree,
      client: Tree
  ): Tree = {
    val jsonSerializer = macroUtils.getJsonSerializer(server)
    val disposableFunctionMethodNameRepository = macroUtils.getDisposableFunctionMethodNameRepository(client)
    val requestJsonHandlerRepository = macroUtils.getRequestJsonHandlerRepository(server)
    val executionContext = macroUtils.getExecutionContext(server)

    def response(id: Tree) =
      q"""
          JsonRpcResultResponse[Unit](
            jsonrpc = Constants.JsonRpc,
            id = $id,
            result = ()
          )
          """

    val maybeErrorJson = macroUtils.createMaybeErrorJsonFromRequestJson(
      server,
      c.Expr[String](q"json"),
      c.Expr[JsonRpcError[String]](q"JsonRpcErrors.internalError")
    )

    q"""
        (json: String) => {
          ..${macroUtils.imports}
          val maybeResponse: Option[String] = $jsonSerializer.deserialize[JsonRpcRequest[Tuple1[String]]](json)
            .flatMap(request => {
              val Tuple1(methodName) = request.params
              $disposableFunctionMethodNameRepository.remove(methodName)
              $requestJsonHandlerRepository.remove(methodName)
              val response = ${response(q"request.id")}
              $jsonSerializer.serialize(response)
            })
            .orElse { $maybeErrorJson }
          Future(maybeResponse)($executionContext)
        }
        """
  }
}
