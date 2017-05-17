package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.BaseSpec
import io.github.shogowada.scala.jsonrpc.Types.JSONSender
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJSONSerializer

import scala.concurrent.Future

class JSONRPCClientTest extends BaseSpec {

  override def newInstance = new JSONRPCClientTest

  var jsonSender: JSONSender = (json: String) => {
    Future(None)
  }
  val client = JSONRPCClient(UpickleJSONSerializer(), (json: String) => jsonSender(json))

  "given I have an API" - {
    trait API {
      def foo(bar: String, baz: Int): Future[String]
    }

    "when I create a client API" - {
      val api = client.createAPI[API]

      "then it should be an instance of the API" in {
        api.isInstanceOf[API] should equal(true)
      }

      "and it fails to send the JSON" - {
        val exception = new IllegalStateException()
        jsonSender = (json: String) => {
          Future.failed(exception)
        }

        val futureResult = api.foo("bar", 1)

        "then it should return the failure" in {
          recoverToSucceededIf[IllegalStateException] {
            futureResult
          }
        }
      }
    }
  }
}
