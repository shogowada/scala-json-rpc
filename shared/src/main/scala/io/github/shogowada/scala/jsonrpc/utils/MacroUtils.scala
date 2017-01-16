package io.github.shogowada.scala.jsonrpc.utils

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction
import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcError, JsonRpcErrorResponse, JsonRpcNotification, JsonRpcRequest}
import io.github.shogowada.scala.jsonrpc.api.JsonRpcMethod

import scala.reflect.macros.blackbox

class MacroUtils[CONTEXT <: blackbox.Context](val c: CONTEXT) {

  import c.universe._

  lazy val imports =
    q"""
        import scala.concurrent.Future
        import scala.util._
        import io.github.shogowada.scala.jsonrpc.Constants
        import io.github.shogowada.scala.jsonrpc.Models._
        import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer._
        """

  lazy val createUuid: c.Expr[String] = c.Expr[String](q"java.util.UUID.randomUUID.toString")

  def getJsonSerializer(prefix: Tree): Tree = q"$prefix.jsonSerializer"

  def getPromisedResponseRepository(prefix: Tree): Tree = q"$prefix.promisedResponseRepository"

  def getMethodNameToHandlerMap(prefix: Tree): Tree = q"$prefix.methodNameToHandlerMap"

  def getSend(prefix: Tree): Tree = q"$prefix.send"

  def getReceive(prefix: Tree): Tree = q"$prefix.receive"

  def getExecutionContext(prefix: Tree): Tree = q"$prefix.executionContext"

  def getJsonRpcApiMethods
  (apiType: Type)
  : Iterable[MethodSymbol] = {
    apiType.decls
        .filter((apiMember: Symbol) => isJsonRpcMethod(apiMember))
        .map((apiMember: Symbol) => apiMember.asMethod)
  }

  private def isJsonRpcMethod(method: Symbol): Boolean = {
    method.isMethod && method.isPublic && !method.isConstructor
  }

  def createClientMethodAsFunction(
      client: Tree,
      jsonRpcMethodName: Tree,
      paramTypes: Seq[Type],
      returnType: Type
  ): Tree = {
    def getParamName(index: Int): TermName = TermName(s"param$index")

    val jsonRpcParameterType: Tree = getJsonRpcParameterType(paramTypes)
    val jsonRpcParameters: Seq[Tree] = paramTypes
        .indices
        .map(index => {
          val paramType = paramTypes(index)
          if (isJsonRpcFunctionType(paramType)) {
            createJsonRpcFunctionParameter(paramType)
          } else {
            q"${getParamName(index)}"
          }
        })

    val jsonRpcParameter = if (jsonRpcParameters.size == 1) {
      val parameter = jsonRpcParameters.head
      q"Tuple1($parameter)"
    } else {
      q"(..$jsonRpcParameters)"
    }

    def createMethodBody: c.Expr[returnType.type] = {
      if (isJsonRpcNotificationMethod(returnType)) {
        createNotificationMethodBody(
          client,
          jsonRpcParameterType,
          jsonRpcMethodName,
          jsonRpcParameter,
          returnType
        )
      } else {
        createRequestMethodBody(
          client,
          jsonRpcParameterType,
          jsonRpcMethodName,
          jsonRpcParameter,
          returnType
        )
      }
    }

    val parameterList: Seq[Tree] = paramTypes.indices
        .map(index => {
          val paramType = paramTypes(index)
          q"${getParamName(index)}: $paramType"
        })

    q"""
        (..$parameterList) => {
          ..${imports}
          $createMethodBody
        }
        """
  }

  def createJsonRpcFunctionParameter(
      jsonRpcFunctionType: Type
  ): Tree = {
    q"$createUuid"
  }

  def createNotificationMethodBody(
      client: Tree,
      jsonRpcParameterType: Tree,
      jsonRpcMethodName: Tree,
      jsonRpcParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = getJsonSerializer(client)
    val send = getSend(client)

    val notification = c.Expr[JsonRpcNotification[jsonRpcParameterType.type]](
      q"""
          JsonRpcNotification[$jsonRpcParameterType](
            jsonrpc = Constants.JsonRpc,
            method = $jsonRpcMethodName,
            params = $jsonRpcParameter
          )
          """
    )

    val notificationJson = c.Expr[String](
      q"$jsonSerializer.serialize($notification).get"
    )

    c.Expr[returnType.type](q"$send($notificationJson)")
  }

  def createRequestMethodBody(
      client: Tree,
      jsonRpcParameterType: Tree,
      jsonRpcMethodName: Tree,
      jsonRpcParameter: Tree,
      returnType: Type
  ): c.Expr[returnType.type] = {
    val jsonSerializer = getJsonSerializer(client)
    val promisedResponseRepository = getPromisedResponseRepository(client)
    val send = getSend(client)
    val receive = getReceive(client)
    val executionContext = getExecutionContext(client)

    val resultType: Type = returnType.typeArgs.head

    val requestId = TermName("requestId")

    val request = c.Expr[JsonRpcRequest[jsonRpcParameterType.type]](
      q"""
          JsonRpcRequest[$jsonRpcParameterType](
            jsonrpc = Constants.JsonRpc,
            id = $requestId,
            method = $jsonRpcMethodName,
            params = $jsonRpcParameter
          )
          """
    )

    val requestJson = c.Expr[String](q"""$jsonSerializer.serialize($request).get""")

    val promisedResponse = TermName("promisedResponse")

    c.Expr[returnType.type](
      q"""
          val $requestId = Left($createUuid)
          val $promisedResponse = $promisedResponseRepository.addAndGet($requestId)

          $send($requestJson).onComplete((tried: Try[Option[String]]) => tried match {
            case Success(Some(responseJson: String)) => $receive(responseJson)
            case Failure(throwable) => $promisedResponse.failure(throwable)
            case _ =>
          })($executionContext)

          $promisedResponse.future
              .map((json: String) => {
                $jsonSerializer.deserialize[JsonRpcResultResponse[$resultType]](json)
                    .map(resultResponse => resultResponse.result)
                    .getOrElse {
                      val maybeResponse = $jsonSerializer.deserialize[JsonRpcErrorResponse[String]](json)
                      throw new JsonRpcException(maybeResponse)
                    }
              })($executionContext)
          """
    )
  }

  def getJsonRpcMethodName(method: MethodSymbol): String = {
    val maybeCustomMethodName: Option[String] = method.annotations
        .find(annotation => annotation.tree.tpe =:= typeOf[JsonRpcMethod])
        .map(annotation => annotation.tree.children.tail.head match {
          case Literal(Constant(name: String)) => name
        })
    maybeCustomMethodName.getOrElse(method.fullName)
  }

  def getJsonRpcParameterType(method: MethodSymbol): Tree = {
    val paramTypes: Seq[Type] = method.paramLists.flatten
        .map(param => param.typeSignature)
    getJsonRpcParameterType(paramTypes)
  }

  def getJsonRpcParameterType(paramTypes: Seq[Type]): Tree = {
    val parameterTypes: Iterable[Type] = paramTypes
        .map(mapSingleJsonRpcParameterType)

    if (parameterTypes.size == 1) {
      val parameterType = parameterTypes.head
      tq"Tuple1[$parameterType]"
    } else {
      tq"(..$parameterTypes)"
    }
  }

  private def mapSingleJsonRpcParameterType(paramType: Type): Type = {
    if (isJsonRpcFunctionType(paramType)) {
      getType[String]
    } else {
      paramType
    }
  }

  def isJsonRpcFunctionType(theType: Type): Boolean = {
    theType <:< getType[JsonRpcFunction[_]]
  }

  def getFunctionTypeOfJsonRpcFunctionType(jsonRpcFunctionType: Type): Type = {
    jsonRpcFunctionType.typeArgs.head
  }

  def isJsonRpcNotificationMethod(method: MethodSymbol): Boolean = {
    isJsonRpcNotificationMethod(method.returnType)
  }

  def isJsonRpcNotificationMethod(returnType: Type): Boolean = {
    returnType =:= getType[Unit]
  }

  def getType[T: c.TypeTag]: Type = {
    typeOf[T]
  }

  def createMaybeErrorJson(
      server: c.Tree,
      json: c.Expr[String],
      jsonRpcError: c.Expr[JsonRpcError[String]]
  ): c.Expr[Option[String]] = {

    val jsonSerializer: Tree = q"$server.jsonSerializer"

    val error = (id: TermName) => c.Expr[JsonRpcErrorResponse[String]](
      q"""
          JsonRpcErrorResponse(
            jsonrpc = Constants.JsonRpc,
            id = $id,
            error = $jsonRpcError
          )
          """
    )

    val maybeErrorJson = (id: TermName) => c.Expr[Option[String]](
      q"""$jsonSerializer.serialize(${error(id)})"""
    )

    c.Expr[Option[String]](
      q"""
          $jsonSerializer.deserialize[JsonRpcId]($json)
            .map(requestId => requestId.id)
            .flatMap(id => {
              ${maybeErrorJson(TermName("id"))}
            })
          """
    )
  }
}

object MacroUtils {
  def apply[CONTEXT <: blackbox.Context](c: CONTEXT) = new MacroUtils[CONTEXT](c)
}
