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

  lazy val uuid: c.Expr[String] = c.Expr[String](q"java.util.UUID.randomUUID.toString")

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
      methodName: String,
      paramTypes: Seq[Type],
      returnType: Type
  ): Tree = {
    def paramName(index: Int): TermName = TermName(s"param$index")

    val parameterType: Tree = getJsonRpcParameterType(paramTypes)
    val parameters: Seq[Tree] = paramTypes
        .indices
        .map(index => {
          val paramType = paramTypes(index)
          if (isJsonRpcFunctionType(paramType)) {
            q"${uuid}"
          } else {
            q"${paramName(index)}"
          }
        })

    val parametersAsTuple = if (parameters.size == 1) {
      val parameter = parameters.head
      q"Tuple1($parameter)"
    } else {
      q"(..$parameters)"
    }

    val jsonSerializer: Tree = q"$client.jsonSerializer"
    val send: Tree = q"$client.send"
    val receive: Tree = q"$client.receive"
    val promisedResponseRepository: Tree = q"$client.promisedResponseRepository"
    val executionContext: Tree = q"$client.executionContext"

    def createNotificationMethodBody: c.Expr[returnType.type] = {
      val notification = c.Expr[JsonRpcNotification[parameterType.type]](
        q"""
            JsonRpcNotification[$parameterType](
              jsonrpc = Constants.JsonRpc,
              method = $methodName,
              params = $parametersAsTuple
            )
            """
      )

      val notificationJson = c.Expr[String](
        q"""
            $jsonSerializer.serialize($notification).get
            """
      )

      c.Expr[returnType.type](
        q"""
            $send($notificationJson)
            """
      )
    }

    def createRequestMethodBody: c.Expr[returnType.type] = {
      val resultType: Type = returnType.typeArgs.head

      val requestId = TermName("requestId")

      val request = c.Expr[JsonRpcRequest[parameterType.type]](
        q"""
            JsonRpcRequest[$parameterType](
              jsonrpc = Constants.JsonRpc,
              id = $requestId,
              method = $methodName,
              params = $parametersAsTuple
            )
            """
      )

      val requestJson = c.Expr[String](
        q"""
            $jsonSerializer.serialize($request).get
            """
      )

      val promisedResponse = TermName("promisedResponse")

      c.Expr[returnType.type](
        q"""
            val $requestId = Left(${uuid})
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

    def createMethodBody: c.Expr[returnType.type] = {
      if (isJsonRpcNotificationMethod(returnType)) {
        createNotificationMethodBody
      } else {
        createRequestMethodBody
      }
    }

    val parameterList: Seq[Tree] = paramTypes.indices
        .map(index => {
          val paramType = paramTypes(index)
          q"${paramName(index)}: $paramType"
        })
    q"""
        (..$parameterList) => {
          ..${imports}
          $createMethodBody
        }
        """
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
