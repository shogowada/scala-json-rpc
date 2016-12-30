package io.github.shogowada.scala.jsonrpc.client

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.scalatest.{Matchers, path}

import scala.concurrent.{ExecutionContext, Future}

class JsonRpcClientTest extends path.FunSpec
    with Matchers {
  override def newInstance: path.FunSpecLike = new JsonRpcClientTest

  implicit val executionContext = ExecutionContext.Implicits.global
  val target = JsonRpcClientBuilder(UpickleJsonSerializer(), (json: String) => {})
      .build

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
