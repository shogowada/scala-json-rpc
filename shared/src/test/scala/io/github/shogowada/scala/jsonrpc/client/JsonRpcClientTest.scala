package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.scalatest.{AsyncFunSpec, Matchers}

import scala.concurrent.Future

class JsonRpcClientTest extends AsyncFunSpec
    with Matchers {

  var jsonSender = (json: String) => {
    Future(None)
  }
  val client = JsonRpcClient(UpickleJsonSerializer(), (json: String) => jsonSender(json))

  describe("given I have an API") {
    trait Api {
      def foo(bar: String, baz: Int): Future[String]
    }

    describe("when I create a client API") {
      val api = client.createApi[Api]

      it("then it should be an instance of the API") {
        api.isInstanceOf[Api] should equal(true)
      }

      describe("and it fails to send the JSON") {
        val exception = new IllegalStateException()
        jsonSender = (json: String) => {
          Future.failed(exception)
        }

        val futureResult = api.foo("bar", 1)

        it("then it should return the failure") {
          recoverToSucceededIf[IllegalStateException] {
            futureResult
          }
        }
      }
    }
  }
}
