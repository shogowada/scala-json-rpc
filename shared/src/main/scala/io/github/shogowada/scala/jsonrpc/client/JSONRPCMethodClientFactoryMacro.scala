package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Models.JSONRPCRequest
import io.github.shogowada.scala.jsonrpc.common.{JSONRPCMacroUtils, JSONRPCParameterFactory, JSONRPCResultFactory}

import scala.reflect.macros.blackbox

class JSONRPCMethodClientFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  private lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  private lazy val parameterFactory = JSONRPCParameterFactory[c.type](c)
  private lazy val resultFactory = JSONRPCResultFactory[c.type](c)

  def createAsFunction(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRPCMethodName: Tree,
      paramTypes: Seq[Type],
      returnType: Type
  ): Tree = {
    val parameterList: Seq[Tree] = paramTypes
        .zipWithIndex
        .map { case (paramType, index) =>
          q"${getParamName(index)}: $paramType"
        }

    val methodBody: c.Expr[returnType.type] = createMethodBody(
      client,
      maybeServer,
      jsonRPCMethodName,
      paramTypes,
      returnType
    )

    q"""
        (..$parameterList) => {
          ..${macroUtils.imports}
          $methodBody
        }
        """
  }

  private def createMethodBody(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRPCMethodName: Tree,
      paramTypes: Seq[Type],
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonRPCParameterType: Tree = parameterFactory.jsonRPCType(paramTypes)
    val jsonRPCParameter = getJSONRPCParameter(client, maybeServer, paramTypes)

    if (macroUtils.isJSONRPCNotificationMethod(returnType)) {
      createNotificationMethodBody(
        client,
        jsonRPCParameterType,
        jsonRPCMethodName,
        jsonRPCParameter,
        returnType
      )
    } else if (macroUtils.isJSONRPCRequestMethod(returnType)) {
      createRequestMethodBody(
        client,
        maybeServer,
        jsonRPCParameterType,
        jsonRPCMethodName,
        jsonRPCParameter,
        returnType
      )
    } else {
      throw new UnsupportedOperationException("JSON RPC method must return either Unit or Future[_]")
    }
  }

  private def getJSONRPCParameter(
      client: Tree,
      maybeServer: Option[Tree],
      paramTypes: Seq[Type]
  ): Tree = {
    def createJSONRPCParameter(paramType: Type, index: Int): Tree = {
      val paramName = getParamName(index)
      parameterFactory.scalaToJSONRPC(client, maybeServer, paramName, paramType)
    }

    val jsonRPCParameters: Seq[Tree] = paramTypes
        .zipWithIndex
        .map { case (paramType, index) => createJSONRPCParameter(paramType, index) }

    if (jsonRPCParameters.size == 1) {
      val parameter = jsonRPCParameters.head
      q"Tuple1($parameter)"
    } else {
      q"(..$jsonRPCParameters)"
    }
  }

  private def getParamName(index: Int): Tree = {
    q"${TermName(s"param_$index")}"
  }

  private def createNotificationMethodBody(
      client: Tree,
      jsonRPCParameterType: Tree,
      jsonRPCMethodName: Tree,
      jsonRPCParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = macroUtils.getJSONSerializer(client)
    val send = macroUtils.getSend(client)

    c.Expr[returnType.type](
      q"""
          val notification = JSONRPCNotification[$jsonRPCParameterType](
              jsonrpc = Constants.JSONRPC,
              method = $jsonRPCMethodName,
              params = $jsonRPCParameter
          )
          $jsonSerializer.serialize(notification)
              .foreach((json: String) => $send(json))
          """
    )
  }

  private def createRequestMethodBody(
      client: Tree,
      maybeServer: Option[Tree],
      jsonRPCParameterType: Tree,
      jsonRPCMethodName: Tree,
      jsonRPCParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = macroUtils.getJSONSerializer(client)
    val promisedResponseRepository = macroUtils.getPromisedResponseRepository(client)
    val send = macroUtils.getSend(client)
    val receive = macroUtils.getReceive(client)
    val executionContext = macroUtils.getExecutionContext(client)

    val requestId = TermName("requestId")

    def request(requestId: TermName) = c.Expr[JSONRPCRequest[jsonRPCParameterType.type]](
      q"""
          JSONRPCRequest[$jsonRPCParameterType](
            jsonrpc = Constants.JSONRPC,
            id = $requestId,
            method = $jsonRPCMethodName,
            params = $jsonRPCParameter
          )
          """
    )

    val promisedResponse = TermName("promisedResponse")

    def responseJSONHandler(): Tree = {
      val resultType: Type = returnType.typeArgs.head
      createResponseJSONHandler(client, maybeServer, resultType)
    }

    c.Expr[returnType.type](
      q"""
          val $requestId = Left(${macroUtils.newUuid})
          $jsonSerializer.serialize(${request(requestId)}) match {
            case Some((requestJSON: String)) => {
              val $promisedResponse = $promisedResponseRepository.addAndGet($requestId)

              $send(requestJSON).onComplete((tried: Try[Option[String]]) => tried match {
                case Success(Some(responseJSON: String)) => $receive(responseJSON)
                case Success(None) =>
                case Failure(throwable) => {
                  $promisedResponseRepository
                      .getAndRemove($requestId)
                      .foreach(promisedResponse => promisedResponse.failure(throwable))
                }
              })($executionContext)

              $promisedResponse.future
                  .map(${responseJSONHandler()})($executionContext)
            }
            case None => {
              val jsonRPCErrorResponse = JSONRPCErrorResponse(
                jsonrpc = Constants.JSONRPC,
                id = $requestId,
                error = JSONRPCErrors.internalError
              )
              Future.failed(new JSONRPCException(Some(jsonRPCErrorResponse)))
            }
          }
          """
    )
  }

  private def createResponseJSONHandler(
      client: Tree,
      maybeServer: Option[Tree],
      resultType: Type
  ): Tree = {
    val jsonSerializer: Tree = macroUtils.getJSONSerializer(client)

    def mapResult(resultResponse: Tree): Tree = {
      val result = q"$resultResponse.result"
      resultFactory.jsonRPCToScala(client, maybeServer, result, resultType)
    }

    val jsonRPCResultType: Tree = resultFactory.jsonRPCType(resultType)

    q"""
        (json: String) => $jsonSerializer.deserialize[JSONRPCResultResponse[$jsonRPCResultType]](json) match {
          case Some(resultResponse) => ${mapResult(q"resultResponse")}
          case None => {
            val maybeResponse = $jsonSerializer.deserialize[JSONRPCErrorResponse[String]](json)
            throw new JSONRPCException(maybeResponse)
          }
        }
        """
  }
}
