package io.github.shogowada.scala.jsonrpc.models

case class JsonRpcRequest
(
    jsonrpc: String,
    id: Either[String, BigDecimal],
    method: String,
    params: Any
)

case class JsonRpcNotification
(
    jsonrpc: String,
    method: String,
    params: Any
)

case class JsonRpcResponse
(
    jsonrpc: String,
    id: Either[String, BigDecimal],
    result: Option[Any],
    error: Option[JsonRpcError]
)

case class JsonRpcError
(
    code: Int,
    message: String,
    data: Option[Any]
)

object JsonRpcErrors {
  lazy val ParseError = JsonRpcError(-32700, "Parse error", Option("Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text."))
  lazy val InvalidRequest = JsonRpcError(-32600, "Invalid Request", Option("The JSON sent is not a valid Request object."))
  lazy val MethodNotFound = JsonRpcError(-32601, "Method not found", Option("The method does not exist / is not available."))
  lazy val InvalidParams = JsonRpcError(-32602, "Invalid params", Option("Invalid method parameter(s)."))
  lazy val InternalError = JsonRpcError(-32603, "Internal error", Option("Internal JSON-RPC error."))
}
