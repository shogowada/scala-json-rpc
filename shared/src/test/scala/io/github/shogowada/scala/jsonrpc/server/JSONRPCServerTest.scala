package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models._
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJSONSerializer
import io.github.shogowada.scala.jsonrpc.{Constants, api}
import org.scalatest.{Assertion, AsyncFunSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait FakeAPI {
  def foo(bar: String, baz: Int): Future[String]

  @api.JSONRPCMethod(name = "bar")
  def bar: Future[String]

  def notify(message: String): Unit
}

class FakeAPIImpl extends FakeAPI {

  import scala.concurrent.ExecutionContext.Implicits.global

  val notifiedMessages = ListBuffer.empty[String]

  override def foo(bar: String, baz: Int): Future[String] = {
    Future(s"$bar$baz")
  }

  override def bar = Future("bar")

  override def notify(message: String): Unit = {
    notifiedMessages += message
  }
}

class JSONRPCServerTest extends AsyncFunSpec
    with Matchers {

  override implicit def executionContext = ExecutionContext.Implicits.global

  describe("given I have an API bound") {
    val api = new FakeAPIImpl

    val jsonSerializer = UpickleJSONSerializer()
    val target = JSONRPCServer(jsonSerializer)
    target.bindAPI[FakeAPI](api)

    def responseShouldEqual[T]
    (
        futureMaybeJSON: Future[Option[String]],
        deserializer: (String) => Option[T],
        expected: T
    ): Future[Assertion] = {
      futureMaybeJSON
          .map((maybeJSON: Option[String]) => {
            maybeJSON.flatMap(json => deserializer(json))
          })
          .map((maybeActual: Option[T]) => maybeActual should equal(Some(expected)))
    }

    def responseShouldEqualError
    (
        futureMaybeJSON: Future[Option[String]],
        expected: JSONRPCErrorResponse[String]
    ): Future[Assertion] = {
      responseShouldEqual(
        futureMaybeJSON,
        (json) => jsonSerializer.deserialize[JSONRPCErrorResponse[String]](json),
        expected
      )
    }

    describe("when I received request for API method") {
      val requestId = Left("request ID")
      val request: JSONRPCRequest[(String, Int)] = JSONRPCRequest(
        jsonrpc = Constants.JSONRPC,
        id = requestId,
        method = classOf[FakeAPI].getName + ".foo",
        params = ("bar", 1)
      )
      val requestJSON: String = jsonSerializer.serialize(request).get

      val futureMaybeResponseJSON: Future[Option[String]] = target.receive(requestJSON)

      it("then it should return the response") {
        responseShouldEqual(
          futureMaybeResponseJSON,
          (json) => jsonSerializer.deserialize[JSONRPCResultResponse[String]](json),
          JSONRPCResultResponse(
            jsonrpc = Constants.JSONRPC,
            id = requestId,
            result = "bar1"
          )
        )
      }
    }

    describe("when I received request for user-named API method") {
      val id = Left("id")
      val request = JSONRPCRequest[Unit](
        jsonrpc = Constants.JSONRPC,
        id = id,
        method = "bar",
        params = ()
      )
      val requestJSON = jsonSerializer.serialize(request).get

      val futureMaybeResponseJSON = target.receive(requestJSON)

      it("then it should return the response") {
        responseShouldEqual(
          futureMaybeResponseJSON,
          (json) => jsonSerializer.deserialize[JSONRPCResultResponse[String]](json),
          JSONRPCResultResponse(
            jsonrpc = Constants.JSONRPC,
            id = id,
            result = "bar"
          )
        )
      }
    }

    describe("when I received notification method") {
      val message = "Hello, World!"
      val notification = JSONRPCNotification[Tuple1[String]](
        jsonrpc = Constants.JSONRPC,
        method = classOf[FakeAPI].getName + ".notify",
        params = Tuple1(message)
      )
      val notificationJSON = jsonSerializer.serialize(notification).get

      val futureMaybeResponseJSON = target.receive(notificationJSON)

      it("then it should notify the server") {
        futureMaybeResponseJSON
            .map(maybeResponse => api.notifiedMessages should equal(List(message)))
      }

      it("then it should not return the response") {
        futureMaybeResponseJSON
            .map(maybeResponse => maybeResponse should equal(None))
      }
    }

    describe("when I receive request with unknown method") {
      val id = Left("id")
      val request = JSONRPCRequest[(String, String)](
        jsonrpc = Constants.JSONRPC,
        id = id,
        method = "unknown",
        params = ("foo", "bar")
      )
      val requestJSON = jsonSerializer.serialize(request).get

      val futureMaybeResponseJSON = target.receive(requestJSON)

      it("then it should respond method not found error") {
        responseShouldEqualError(
          futureMaybeResponseJSON,
          JSONRPCErrorResponse(
            jsonrpc = Constants.JSONRPC,
            id = id,
            error = JSONRPCErrors.methodNotFound
          )
        )
      }
    }

    describe("when I receive JSON without method name") {
      val id = Left("id")
      val requestJSON = """{"jsonrpc":"2.0","id":"id"}"""
      val futureMaybeResponseJSON = target.receive(requestJSON)

      it("then it should respond JSON parse error") {
        responseShouldEqualError(
          futureMaybeResponseJSON,
          JSONRPCErrorResponse(
            jsonrpc = Constants.JSONRPC,
            id = id,
            error = JSONRPCErrors.parseError
          )
        )
      }
    }

    describe("when I receive request with mismatching JSON-RPC version") {
      val id = Left("id")
      val requestJSON = jsonSerializer.serialize(
        JSONRPCRequest(
          jsonrpc = "1.0",
          id = id,
          method = "foo",
          params = ("bar", "baz")
        )
      ).get
      val futureMaybeResponseJSON = target.receive(requestJSON)

      it("then it should respond invalid request") {
        responseShouldEqualError(
          futureMaybeResponseJSON,
          JSONRPCErrorResponse(
            jsonrpc = Constants.JSONRPC,
            id = id,
            error = JSONRPCErrors.invalidRequest
          )
        )
      }
    }

    describe("when I receive request with invalid params") {
      val id = Left("id")
      val request: JSONRPCRequest[Tuple1[String]] = JSONRPCRequest(
        jsonrpc = Constants.JSONRPC,
        id = id,
        method = classOf[FakeAPI].getName + ".foo",
        params = Tuple1("bar")
      )
      val requestJSON = jsonSerializer.serialize(request).get

      val futureMaybeResponseJSON = target.receive(requestJSON)

      it("then it should respond invalid params") {
        responseShouldEqualError(
          futureMaybeResponseJSON,
          JSONRPCErrorResponse(
            jsonrpc = Constants.JSONRPC,
            id = id,
            error = JSONRPCErrors.invalidParams
          )
        )
      }
    }
  }
}
