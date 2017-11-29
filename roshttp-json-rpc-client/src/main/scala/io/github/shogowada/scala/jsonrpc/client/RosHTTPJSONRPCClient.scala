package io.github.shogowada.scala.jsonrpc.client

import monix.execution.Scheduler
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.ByteBufferBody
import io.github.shogowada.scala.jsonrpc.Types.JSONSender

class RosHTTPJSONRPCClient[JSONSerializerInUse <: JSONSerializer](
  jsonSerializer: JSONSerializerInUse,
  jsonRPCEndpoint: String,
  httpHeaders: Seq[(String, String)]
)(implicit scheduler: Scheduler)
    extends JSONRPCClient[JSONSerializerInUse](
  jsonSerializer,
  jsonSender,
  scheduler
)
{
  val jsonSender: JSONSender = { json ⇒
    HttpRequest(jsonRPCEndpoint)
      .withHeaders(httpHeaders)
      .post(
        ByteBufferBody(
          data = java.nio.ByteBuffer.wrap(json.getBytes),
          contentType = "application/json; charset=utf-8"
        )
      )
  }
}

object RosHTTPJSONRPCClient {
  def apply(
    jsonSerializer: JSONSerializer,
    jsonRPCEndpoint: String,
    httpHeaders: (String, String)*
  ): RosHTTPJSONRPCClient = {
    new RosHTTPJSONRPCClient(
      jsonSerializer,
      jsonRPCEndpoint,
      httpHeaders
    )
  }
}
