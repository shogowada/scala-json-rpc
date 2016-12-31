package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcNotification, JsonRpcRequest, JsonRpcRequestId}
import io.github.shogowada.scala.jsonrpc.Types.JsonSender
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import io.github.shogowada.scala.jsonrpc.utils.MacroUtils

import scala.concurrent.{Future, Promise}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class JsonRpcClient[JSON_SERIALIZER <: JsonSerializer]
(
    val jsonSerializer: JSON_SERIALIZER,
    val jsonSender: JsonSender
) {
  val promisedResponseRepository = new JsonRpcPromisedResponseRepository

  def createApi[API]: API = macro JsonRpcClientMacro.createApi[API]

  def receive(json: String): Unit = macro JsonRpcClientMacro.receive
}

object JsonRpcClientMacro {
  def createApi[API: c.WeakTypeTag]
  (c: blackbox.Context)
  : c.Expr[API] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    val memberFunctions = createMemberFunctions[c.type, API](c)
    c.Expr[API](
      q"""
          new {} with $apiType {
            ..$memberFunctions
          }
          """
    )
  }

  private def createMemberFunctions[CONTEXT <: blackbox.Context, API: c.WeakTypeTag]
  (c: CONTEXT)
  : Iterable[c.Tree] = {
    import c.universe._
    val apiType: Type = weakTypeOf[API]
    MacroUtils[c.type](c).getApiMethods(apiType)
        .map((apiMethod: MethodSymbol) => createMemberFunction[c.type](c)(apiMethod))
  }

  private def createMemberFunction[CONTEXT <: blackbox.Context]
  (c: CONTEXT)
  (apiMethod: c.universe.MethodSymbol)
  : c.Tree = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val name: TermName = apiMethod.name
    val methodName: String = macroUtils.getMethodName(apiMethod)
    val parameterLists: List[List[Tree]] =
      apiMethod.paramLists.map((parameterList: List[Symbol]) => {
        parameterList.map((parameter: Symbol) => {
          q"${parameter.name.toTermName}: ${parameter.typeSignature}"
        })
      })
    val parameterType: Tree = macroUtils.getParameterType(apiMethod)
    val parameters: Seq[TermName] = apiMethod.paramLists.flatMap(parameterList => {
      parameterList.map(parameter => {
        parameter.asTerm.name
      })
    })
    val parametersAsTuple = if (parameters.size == 1) {
      val parameter = parameters.head
      q"Tuple1($parameter)"
    } else {
      q"(..$parameters)"
    }
    val returnType: Type = apiMethod.returnType

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"
    val jsonSender: Tree = q"${c.prefix.tree}.jsonSender"
    val promisedResponseRepository: Tree = q"${c.prefix.tree}.promisedResponseRepository"

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
            $jsonSender($notificationJson)
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
            val $requestId = Left(java.util.UUID.randomUUID.toString)
            val $promisedResponse = $promisedResponseRepository.addAndGet($requestId)

            $jsonSender($requestJson).onComplete {
              case Success(Some(responseJson: String)) => ${c.prefix.tree}.receive(responseJson)
              case _ =>
            }

            $promisedResponse.future
                .map((json: String) => {
                  $jsonSerializer.deserialize[JsonRpcResultResponse[$resultType]](json)
                      .map(resultResponse => resultResponse.result)
                      .orElse {
                        $jsonSerializer.deserialize[JsonRpcErrorResponse[String]](json)
                          .map(errorResponse => {
                            throw new JsonRpcException(errorResponse)
                          })
                      }
                      .get
                })
            """
      )
    }

    def createMethodBody: c.Expr[returnType.type] = {
      if (macroUtils.isNotificationMethod(apiMethod)) {
        createNotificationMethodBody
      } else {
        createRequestMethodBody
      }
    }

    q"""
        override def $name(...$parameterLists): $returnType = {
          ..${macroUtils.imports}
          $createMethodBody
        }
        """
  }

  def receive
  (c: blackbox.Context)
  (json: c.Expr[String])
  : c.Expr[Unit] = {
    import c.universe._

    val macroUtils = MacroUtils[c.type](c)

    val jsonSerializer: Tree = q"${c.prefix.tree}.jsonSerializer"
    val promisedResponseRepository: Tree = q"${c.prefix.tree}.promisedResponseRepository"

    val maybeJsonRpcRequestId = c.Expr[JsonRpcRequestId](
      q"""
          $jsonSerializer.deserialize[JsonRpcRequestId]($json)
              .filter(requestId => requestId.jsonrpc == Constants.JsonRpc)
          """
    )

    val maybePromisedResponse = c.Expr[Option[Promise[String]]](
      q"""
          $maybeJsonRpcRequestId
              .flatMap(requestId => $promisedResponseRepository.getAndRemove(requestId.id))
          """
    )

    c.Expr[Unit](
      q"""
          ..${macroUtils.imports}
          $maybePromisedResponse
              .foreach(promisedResponse => promisedResponse.success($json))
          """
    )
  }
}
