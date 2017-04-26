package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcErrorResponse, JsonRpcErrors, JsonRpcException}
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.scalatest.{AsyncFunSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class ServerAndClientTest extends AsyncFunSpec
    with Matchers {

  override implicit def executionContext = ExecutionContext.Implicits.global

  val jsonSerializer = UpickleJsonSerializer()

  describe("given I have APIs") {

    trait CalculatorAPI {
      def add(lhs: Int, rhs: Int): Future[Int]

      def subtract(lhs: Int, rhs: Int): Future[Int]
    }

    trait GreeterAPI {
      @api.JsonRpcMethod("greet")
      def greet(greeting: String): Unit
    }

    class CalculatorAPIImpl extends CalculatorAPI {
      override def add(lhs: Int, rhs: Int): Future[Int] = Future {
        lhs + rhs
      }

      override def subtract(lhs: Int, rhs: Int): Future[Int] = Future {
        lhs - rhs
      }
    }

    class GreeterAPIImpl extends GreeterAPI {
      var greetings: Seq[String] = Seq.empty

      override def greet(greeting: String): Unit = {
        greetings = greetings :+ greeting
      }
    }

    val greeterAPIServer = new GreeterAPIImpl()

    val server = JsonRpcServer(jsonSerializer)
    server.bindAPI[CalculatorAPI](new CalculatorAPIImpl)
    server.bindAPI[GreeterAPI](greeterAPIServer)

    val client = JsonRpcClient(
      jsonSerializer,
      (json: String) => server.receive(json)
    )

    describe("when I am using calculator API") {
      val calculatorAPI = client.createAPI[CalculatorAPI]

      describe("when I add 2 values") {
        val futureResult = calculatorAPI.add(1, 2)

        it("then it should add the 2 values") {
          futureResult.map(result => result should equal(3))
        }
      }

      describe("when I subtract one value from the other") {
        val futureResult = calculatorAPI.subtract(1, 2)

        it("then it should subtract the value") {
          futureResult.map(result => result should equal(-1))
        }
      }
    }

    describe("when I am using greeter API") {
      val greeterAPI = client.createAPI[GreeterAPI]

      describe("when I greet") {
        val greeting = "Hello, World!"
        greeterAPI.greet(greeting)

        it("then it should greet the server") {
          greeterAPIServer.greetings should equal(List(greeting))
        }
      }
    }

    describe("when I am using invalid API") {
      trait InvalidAPI {
        def invalidRequest: Future[String]
      }

      val invalidAPI = client.createAPI[InvalidAPI]

      describe("when I send request") {
        val response = invalidAPI.invalidRequest
        it("then it should response error") {
          response.failed
              .map {
                case exception: JsonRpcException[_] =>
                  exception.maybeResponse should matchPattern {
                    case Some(JsonRpcErrorResponse(Constants.JsonRpc, _, JsonRpcErrors.methodNotFound)) =>
                  }
                case exception => fail("It should have failed with JsonRpcErrorException, but failed with " + exception)
              }
        }
      }
    }
  }
}
