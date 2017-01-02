package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Types.Id

import scala.language.experimental.macros

object Models {

  case class JsonRpcMethod(jsonrpc: String, method: String)

  case class JsonRpcId(jsonrpc: String, id: Id)

  case class JsonRpcRequest[PARAMS]
  (
      jsonrpc: String,
      id: Id,
      method: String,
      params: PARAMS
  )

  case class JsonRpcNotification[PARAMS]
  (
      jsonrpc: String,
      method: String,
      params: PARAMS
  )

  case class JsonRpcResultResponse[RESULT]
  (
      jsonrpc: String,
      id: Id,
      result: RESULT
  )

  case class JsonRpcErrorResponse[+ERROR]
  (
      jsonrpc: String,
      id: Id,
      error: JsonRpcError[ERROR]
  )

  case class JsonRpcError[+ERROR]
  (
      code: Int,
      message: String,
      data: Option[ERROR]
  )

  object JsonRpcErrors {
    lazy val parseError = JsonRpcError(-32700, "Parse error", Option("Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text."))
    lazy val invalidRequest = JsonRpcError(-32600, "Invalid Request", Option("The JSON sent is not a valid Request object."))
    lazy val methodNotFound = JsonRpcError(-32601, "Method not found", Option("The method does not exist / is not available."))
    lazy val invalidParams = JsonRpcError(-32602, "Invalid params", Option("Invalid method parameter(s)."))
    lazy val internalError = JsonRpcError(-32603, "Internal error", Option("Internal JSON-RPC error."))
  }

  class JsonRpcException[+ERROR](val maybeResponse: Option[JsonRpcErrorResponse[ERROR]]) extends RuntimeException

}
