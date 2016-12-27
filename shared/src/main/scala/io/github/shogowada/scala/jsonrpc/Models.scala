package io.github.shogowada.scala.jsonrpc

import scala.language.experimental.macros

object Models {

  case class JsonRpcMethod(jsonrpc: String, method: String)

  case class JsonRpcRequest[PARAMS]
  (
      jsonrpc: String,
      id: Either[String, BigDecimal],
      method: String,
      params: PARAMS
  )

  case class JsonRpcNotification[PARAMS]
  (
      jsonrpc: String,
      method: String,
      params: PARAMS
  )

  case class JsonRpcResponse
  (
      jsonrpc: String,
      id: Either[String, BigDecimal]
  )

  case class JsonRpcResultResponse[RESULT]
  (
      jsonrpc: String,
      id: Either[String, BigDecimal],
      result: RESULT
  )

  case class JsonRpcErrorResponse[ERROR]
  (
      jsonrpc: String,
      id: Either[String, BigDecimal],
      error: JsonRpcError[ERROR]
  )

  case class JsonRpcError[ERROR]
  (
      code: Int,
      message: String,
      data: Option[ERROR]
  )

  object JsonRpcError {
    def apply(code: Int, message: String): JsonRpcError[String] =
      JsonRpcError[String](code, message, None)

    def apply[ERROR](code: Int, message: String, data: ERROR): JsonRpcError[ERROR] =
      JsonRpcError[ERROR](code, message, Option(data))
  }

  object JsonRpcErrors {
    lazy val parseError = JsonRpcError(-32700, "Parse error", "Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text.")
    lazy val invalidRequest = JsonRpcError(-32600, "Invalid Request", "The JSON sent is not a valid Request object.")
    lazy val methodNotFound = JsonRpcError(-32601, "Method not found", "The method does not exist / is not available.")
    lazy val invalidParams = JsonRpcError(-32602, "Invalid params", "Invalid method parameter(s).")
    lazy val internalError = JsonRpcError(-32603, "Internal error", "Internal JSON-RPC error.")
  }

}