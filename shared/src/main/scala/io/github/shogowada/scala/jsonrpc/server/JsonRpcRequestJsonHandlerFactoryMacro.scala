package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcError
import io.github.shogowada.scala.jsonrpc.client.JsonRpcFunctionClientFactoryMacro
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer.RequestJsonHandler
import io.github.shogowada.scala.jsonrpc.utils.JsonRpcMacroUtils

import scala.reflect.macros.blackbox

class JsonRpcRequestJsonHandlerFactoryMacro[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val macroUtils = JsonRpcMacroUtils[c.type](c)
  lazy val jsonRpcFunctionClientFactoryMacro = new JsonRpcFunctionClientFactoryMacro[c.type](c)
  lazy val jsonRpcFunctionServerFactoryMacro = new JsonRpcFunctionServerFactoryMacro[c.type](c)

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

  def createFromJsonRpcFunction(
      client: Tree,
      server: Tree,
      jsonRpcFunction: TermName,
      jsonRpcFunctionType: Type
  ): c.Expr[RequestJsonHandler] = {
    val paramTypes: Seq[Type] = jsonRpcFunctionType.typeArgs.init
    val returnType: Type = jsonRpcFunctionType.typeArgs.last

    create(RequestJsonHandlerContext(
      server = server,
      maybeClient = Some(client),
      methodName = q"$jsonRpcFunction",
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

    val returnsJsonRpcFunction = handlerContext.returnType.typeArgs.headOption
        .exists(macroUtils.isJsonRpcFunctionType)

    if (returnsJsonRpcFunction) {
      def jsonRpcFunctionServer(jsonRpcFunction: TermName): c.Expr[String] =
        handlerContext.maybeClient
            .map(client => jsonRpcFunctionServerFactoryMacro.getOrCreate(client, handlerContext.server, jsonRpcFunction, handlerContext.returnType))
            .getOrElse(throw new UnsupportedOperationException("To return JsonRpcFunction, you need to bind the API to JsonRpcServerAndClient."))
      q"""
          $realMethodInvocation
              .map(jsonRpcFunction => ${jsonRpcFunctionServer(TermName("jsonRpcFunction"))})($executionContext)
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
      if (macroUtils.isJsonRpcFunctionType(parameterType)) {
        handlerContext.maybeClient
            .map(client => jsonRpcFunctionClientFactoryMacro.getOrCreate(handlerContext.server, client, parameterType, argument))
            .getOrElse(throw new UnsupportedOperationException("To use JsonRpcFunction, you need to bind the API to JsonRpcServerAndClient."))
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
    val jsonRpcFunctionMethodNameRepository = macroUtils.getJsonRpcFunctionMethodNameRepository(client)
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
              $jsonRpcFunctionMethodNameRepository.remove(methodName)
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
