package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.scalatest.{Matchers, path}

import scala.concurrent.Future

class FakeJsonSender extends JsonSender {
  override def send(json: String): Unit = {
    throw new UnsupportedOperationException
  }
}

class JsonRpcClientTest extends path.FunSpec
    with Matchers {
  override def newInstance: path.FunSpecLike = new JsonRpcClientTest

  val jsonSender = new FakeJsonSender
  val target = JsonRpcClient(UpickleJsonSerializer(), jsonSender)

  describe("given I have an API") {
    trait Api {
      def foo(bar: String, baz: Int): Future[String]
    }

    describe("when I create a client API") {
      val api = target.createApi[Api]

      it("then it should be an instance of the API") {
        api.isInstanceOf[Api] should equal(true)
      }
    }
  }
}
