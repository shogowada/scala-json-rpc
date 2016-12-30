package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServerBuilder
import org.scalatest.{AsyncFunSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class ClientAndServerTest extends AsyncFunSpec
    with Matchers {

  override implicit def executionContext = ExecutionContext.Implicits.global

  val jsonSerializer = UpickleJsonSerializer()

  describe("given I have APIs") {

    trait CalculatorApi {
      def add(lhs: Int, rhs: Int): Future[Int]

      def subtract(lhs: Int, rhs: Int): Future[Int]
    }

    trait GreeterApi {
      def greet(greeting: String): Unit
    }

    class CalculatorApiImpl extends CalculatorApi {
      override def add(lhs: Int, rhs: Int): Future[Int] = {
        Future(lhs + rhs)
      }

      override def subtract(lhs: Int, rhs: Int): Future[Int] = {
        Future(lhs - rhs)
      }
    }

    class GreeterApiImpl extends GreeterApi {
      val greetings = ListBuffer.empty[String]

      override def greet(greeting: String): Unit = {
        greetings += greeting
      }
    }

    val greeterApiServer = new GreeterApiImpl

    val serverBuilder = JsonRpcServerBuilder(jsonSerializer)
    serverBuilder.bindApi[CalculatorApi](new CalculatorApiImpl)
    serverBuilder.bindApi[GreeterApi](greeterApiServer)

    val server = serverBuilder.build
    val client: JsonRpcClient[UpickleJsonSerializer] = JsonRpcClient(
      jsonSerializer,
      (json: String) => server.receive(json)
    )

    describe("when I am using calculator API") {
      val calculatorApi = client.createApi[CalculatorApi]

      describe("when I add 2 values") {
        val futureResult = calculatorApi.add(1, 2)

        it("then it should add the 2 values") {
          futureResult.map(result => result should equal(3))
        }
      }

      describe("when I subtract one value from the other") {
        val futureResult = calculatorApi.subtract(1, 2)

        it("then it should subtract the value") {
          futureResult.map(result => result should equal(-1))
        }
      }
    }

    describe("when I am using greeter API") {
      val greeterApi = client.createApi[GreeterApi]

      describe("when I greet") {
        val greeting = "Hello, World!"
        greeterApi.greet(greeting)

        it("then it should greet the server") {
          greeterApiServer.greetings should equal(List(greeting))
        }
      }
    }
  }
}
