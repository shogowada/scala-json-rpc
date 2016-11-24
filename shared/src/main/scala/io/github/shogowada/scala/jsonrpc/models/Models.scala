package io.github.shogowada.scala.jsonrpc.models

object Models {
  val jsonRpc = "2.0"

  type Id = Either[String, BigDecimal]
  type Params = Either[Seq[Any], Map[String, Any]]

  case class JsonRpcMessage(jsonrpc: String = jsonRpc)

  case class JsonRpcRequest
  (
      id: Id,
      method: String,
      params: Params
  ) extends JsonRpcMessage

  case class JsonRpcNotification
  (
      method: String,
      params: Params
  ) extends JsonRpcMessage

  case class JsonRpcResponse
  (
      id: Option[Id],
      result: Option[Any],
      error: Option[JsonRpcError]
  ) extends JsonRpcMessage

  object JsonRpcResponse {
    def apply(error: JsonRpcError) = JsonRpcResponse(id = None, result = None, Some(error))
  }

  case class JsonRpcError
  (
      code: Int,
      message: String,
      data: Option[Any]
  )

  object JsonRpcError {
    def apply(code: Int, message: String) = JsonRpcError(code, message, None)

    def apply(code: Int, message: String, data: Any) = JsonRpcError(code, message, Option(data))
  }

  object JsonRpcErrors {
    lazy val parseError = JsonRpcError(-32700, "Parse error", "Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text.")
    lazy val invalidRequest = JsonRpcError(-32600, "Invalid Request", "The JSON sent is not a valid Request object.")
    lazy val methodNotFound = JsonRpcError(-32601, "Method not found", "The method does not exist / is not available.")
    lazy val invalidParams = JsonRpcError(-32602, "Invalid params", "Invalid method parameter(s).")
    lazy val internalError = JsonRpcError(-32603, "Internal error", "Internal JSON-RPC error.")
  }

}
