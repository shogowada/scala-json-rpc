package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JSONRPCError
import io.github.shogowada.scala.jsonrpc.client.DisposableFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer.RequestJSONHandler
import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

class JSONRPCRequestJSONHandlerFactoryMacro[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val disposableFunctionClientFactoryMacro = new DisposableFunctionClientFactoryMacro[c.type](c)
  private lazy val disposableFunctionServerFactoryMacro = new DisposableFunctionServerFactoryMacro[c.type](c)

  case class RequestJSONHandlerContext(
      server: c.Tree,
      maybeClient: Option[c.Tree],
      methodName: c.Tree,
      parameterTypeLists: Seq[Seq[c.Type]],
      returnType: c.Type
  )

  def createFromAPIMethod[API](
      server: c.Tree,
      maybeClient: Option[c.Tree],
      api: c.Expr[API],
      method: c.universe.MethodSymbol
  ): c.Expr[RequestJSONHandler] = {
    val parameterTypeLists: List[List[Type]] = method.asMethod.paramLists
        .map(parameters => parameters.map(parameter => parameter.typeSignature))

    create(RequestJSONHandlerContext(
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
  ): c.Expr[RequestJSONHandler] = {
    val paramTypes: Seq[Type] = disposableFunctionType.typeArgs.init
    val returnType: Type = disposableFunctionType.typeArgs.last

    create(RequestJSONHandlerContext(
      server = server,
      maybeClient = Some(client),
      methodName = q"$disposableFunction",
      parameterTypeLists = Seq(paramTypes),
      returnType = returnType
    ))
  }

  private def create(handlerContext: RequestJSONHandlerContext): c.Expr[RequestJSONHandler] = {
    if (macroUtils.isJSONRPCNotificationMethod(handlerContext.returnType)) {
      createNotificationHandler(handlerContext)
    } else {
      createRequestHandler(handlerContext)
    }
  }

  private def createNotificationHandler(handlerContext: RequestJSONHandlerContext): c.Expr[RequestJSONHandler] = {
    val jsonSerializer = macroUtils.getJSONSerializer(handlerContext.server)
    val executionContext = macroUtils.getExecutionContext(handlerContext.server)

    val params = TermName("params")

    val jsonRPCParameterType: Tree = macroUtils.getJSONRPCParameterType(handlerContext.parameterTypeLists.flatten)

    def methodInvocation(params: TermName) = createMethodInvocation(handlerContext, params)

    c.Expr[RequestJSONHandler](
      q"""
          (json: String) => {
            ..${macroUtils.imports}
            $jsonSerializer.deserialize[JSONRPCNotification[$jsonRPCParameterType]](json)
              .foreach(notification => {
                val $params = notification.params
                ${methodInvocation(params)}
              })
            Future(None)($executionContext)
          }
          """
    )
  }

  private def createRequestHandler(handlerContext: RequestJSONHandlerContext): c.Expr[RequestJSONHandler] = {
    val jsonSerializer = macroUtils.getJSONSerializer(handlerContext.server)
    val executionContext = macroUtils.getExecutionContext(handlerContext.server)

    val json = TermName("json")
    val request = TermName("request")
    val params = TermName("params")

    def maybeInvalidParamsErrorJSON(json: TermName): c.Expr[Option[String]] =
      macroUtils.createMaybeErrorJSONFromRequestJSON(
        handlerContext.server,
        c.Expr[String](q"$json"),
        c.Expr[JSONRPCError[String]](q"JSONRPCErrors.invalidParams")
      )

    val jsonRPCParameterType: Tree = macroUtils.getJSONRPCParameterType(handlerContext.parameterTypeLists.flatten)

    def methodInvocation(params: TermName): Tree = createMethodInvocation(handlerContext, params)

    c.Expr[RequestJSONHandler](
      q"""
          ($json: String) => {
            ..${macroUtils.imports}
            $jsonSerializer.deserialize[JSONRPCRequest[$jsonRPCParameterType]]($json)
              .map(($request: JSONRPCRequest[$jsonRPCParameterType]) => {
                val $params = $request.params
                ${methodInvocation(params)}
                  .map((result) => JSONRPCResultResponse(
                    jsonrpc = Constants.JSONRPC,
                    id = $request.id,
                    result = result
                  ))($executionContext)
                  .map((response) => $jsonSerializer.serialize(response))($executionContext)
              })
              .getOrElse(Future(${maybeInvalidParamsErrorJSON(json)})($executionContext))
          }
          """
    )
  }

  private def createMethodInvocation(
      handlerContext: RequestJSONHandlerContext,
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
            .getOrElse(throw new UnsupportedOperationException("To return DisposableFunction, you need to bind the API to JSONRPCServerAndClient."))
      q"""
          $realMethodInvocation
              .map(disposableFunction => ${disposableFunctionServer(TermName("disposableFunction"))})($executionContext)
          """
    } else {
      realMethodInvocation
    }
  }

  private def createArguments(
      handlerContext: RequestJSONHandlerContext,
      params: TermName
  ): Seq[Tree] = {
    val parameterTypes: Seq[Type] = handlerContext.parameterTypeLists.flatten

    def createArgument(parameterType: Type, index: Int): Tree = {
      val fieldName = TermName(s"_${index + 1}")
      val argument = q"$params.$fieldName"
      if (macroUtils.isDisposableFunctionType(parameterType)) {
        handlerContext.maybeClient
            .map(client => disposableFunctionClientFactoryMacro.getOrCreate(handlerContext.server, client, parameterType, argument))
            .getOrElse(throw new UnsupportedOperationException("To use DisposableFunction, you need to bind the API to JSONRPCServerAndClient."))
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
    val jsonSerializer = macroUtils.getJSONSerializer(server)
    val disposableFunctionMethodNameRepository = macroUtils.getDisposableFunctionMethodNameRepository(client)
    val requestJSONHandlerRepository = macroUtils.getRequestJSONHandlerRepository(server)
    val executionContext = macroUtils.getExecutionContext(server)

    def response(id: Tree) =
      q"""
          JSONRPCResultResponse[Unit](
            jsonrpc = Constants.JSONRPC,
            id = $id,
            result = ()
          )
          """

    val maybeErrorJSON = macroUtils.createMaybeErrorJSONFromRequestJSON(
      server,
      c.Expr[String](q"json"),
      c.Expr[JSONRPCError[String]](q"JSONRPCErrors.internalError")
    )

    q"""
        (json: String) => {
          ..${macroUtils.imports}
          val maybeResponse: Option[String] = $jsonSerializer.deserialize[JSONRPCRequest[Tuple1[String]]](json)
            .flatMap(request => {
              val Tuple1(methodName) = request.params
              $disposableFunctionMethodNameRepository.remove(methodName)
              $requestJSONHandlerRepository.remove(methodName)
              val response = ${response(q"request.id")}
              $jsonSerializer.serialize(response)
            })
            .orElse { $maybeErrorJSON }
          Future(maybeResponse)($executionContext)
        }
        """
  }
}
