package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models.JSONRPCError
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer.RequestJSONHandler
import io.github.shogowada.scala.jsonrpc.common.{JSONRPCMacroUtils, JSONRPCResultFactory}

import scala.reflect.macros.blackbox

class JSONRPCRequestJSONHandlerFactoryMacro[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val parameterFactory = JSONRPCParameterFactory[c.type](c)
  private lazy val resultFactory = JSONRPCResultFactory[c.type](c)

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
    val methodInvocation = if (handlerContext.parameterTypeLists.isEmpty) {
      q"${handlerContext.methodName}"
    } else {
      q"${handlerContext.methodName}(..${createArguments(handlerContext, params)})"
    }

    resultFactory.scalaToJSONRPC(
      server = handlerContext.server,
      maybeClient = handlerContext.maybeClient,
      returnValue = methodInvocation,
      returnValueType = handlerContext.returnType
    )
  }

  private def createArguments(
      handlerContext: RequestJSONHandlerContext,
      params: TermName
  ): Seq[Tree] = {
    val parameterTypes: Seq[Type] = handlerContext.parameterTypeLists.flatten

    def createParameter(parameterType: Type, index: Int): Tree = {
      val fieldName = TermName(s"_${index + 1}")
      val argument = q"$params.$fieldName"
      parameterFactory.create(
        server = handlerContext.server,
        maybeClient = handlerContext.maybeClient,
        argument = argument,
        argumentType = parameterType
      )
    }

    parameterTypes
        .zipWithIndex
        .map { case (parameterType, index) => createParameter(parameterType, index) }
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
