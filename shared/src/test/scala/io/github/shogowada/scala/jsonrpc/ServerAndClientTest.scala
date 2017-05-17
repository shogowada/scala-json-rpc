package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Models.{JSONRPCErrorResponse, JSONRPCErrors, JSONRPCException}
import io.github.shogowada.scala.jsonrpc.client.JSONRPCClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJSONSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer

import scala.concurrent.{ExecutionContext, Future}

class ServerAndClientTest extends BaseSpec {

  override def newInstance = new ServerAndClientTest

  override implicit def executionContext = ExecutionContext.Implicits.global

  val jsonSerializer = UpickleJSONSerializer()

  "given I have APIs" - {

    trait CalculatorAPI {
      def add(lhs: Int, rhs: Int): Future[Int]

      def subtract(lhs: Int, rhs: Int): Future[Int]
    }

    trait GreeterAPI {
      @api.JSONRPCMethod("greet")
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

    val server = JSONRPCServer(jsonSerializer)
    server.bindAPI[CalculatorAPI](new CalculatorAPIImpl)
    server.bindAPI[GreeterAPI](greeterAPIServer)

    val client = JSONRPCClient(
      jsonSerializer,
      (json: String) => server.receive(json)
    )

    "when I am using calculator API" - {
      val calculatorAPI = client.createAPI[CalculatorAPI]

      "when I add 2 values" - {
        val futureResult = calculatorAPI.add(1, 2)

        "then it should add the 2 values" in {
          futureResult.map(result => result should equal(3))
        }
      }

      "when I subtract one value from the other" - {
        val futureResult = calculatorAPI.subtract(1, 2)

        "then it should subtract the value" in {
          futureResult.map(result => result should equal(-1))
        }
      }
    }

    "when I am using greeter API" - {
      val greeterAPI = client.createAPI[GreeterAPI]

      "when I greet" - {
        val greeting = "Hello, World!"
        greeterAPI.greet(greeting)

        "then it should greet the server" in {
          greeterAPIServer.greetings should equal(List(greeting))
        }
      }
    }

    "when I am using invalid API" - {
      trait InvalidAPI {
        def invalidRequest: Future[String]
      }

      val invalidAPI = client.createAPI[InvalidAPI]

      "when I send request" - {
        val response = invalidAPI.invalidRequest
        "then it should response error" in {
          response.failed
              .map {
                case exception: JSONRPCException[_] =>
                  exception.maybeResponse should matchPattern {
                    case Some(JSONRPCErrorResponse(Constants.JSONRPC, _, JSONRPCErrors.methodNotFound)) =>
                  }
                case exception => fail("It should have failed with JSONRPCErrorException, but failed with " + exception)
              }
        }
      }
    }
  }
}
