package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Models._
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
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

    val jsonSerializer = UpickleJsonSerializer()
    val target = JSONRPCServer(jsonSerializer)
    target.bindAPI[FakeAPI](api)

    def responseShouldEqual[T]
    (
        futureMaybeJson: Future[Option[String]],
        deserializer: (String) => Option[T],
        expected: T
    ): Future[Assertion] = {
      futureMaybeJson
          .map((maybeJson: Option[String]) => {
            maybeJson.flatMap(json => deserializer(json))
          })
          .map((maybeActual: Option[T]) => maybeActual should equal(Some(expected)))
    }

    def responseShouldEqualError
    (
        futureMaybeJson: Future[Option[String]],
        expected: JSONRPCErrorResponse[String]
    ): Future[Assertion] = {
      responseShouldEqual(
        futureMaybeJson,
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
      val requestJson: String = jsonSerializer.serialize(request).get

      val futureMaybeResponseJson: Future[Option[String]] = target.receive(requestJson)

      it("then it should return the response") {
        responseShouldEqual(
          futureMaybeResponseJson,
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
      val requestJson = jsonSerializer.serialize(request).get

      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should return the response") {
        responseShouldEqual(
          futureMaybeResponseJson,
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
      val notificationJson = jsonSerializer.serialize(notification).get

      val futureMaybeResponseJson = target.receive(notificationJson)

      it("then it should notify the server") {
        futureMaybeResponseJson
            .map(maybeResponse => api.notifiedMessages should equal(List(message)))
      }

      it("then it should not return the response") {
        futureMaybeResponseJson
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
      val requestJson = jsonSerializer.serialize(request).get

      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should respond method not found error") {
        responseShouldEqualError(
          futureMaybeResponseJson,
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
      val requestJson = """{"jsonrpc":"2.0","id":"id"}"""
      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should respond JSON parse error") {
        responseShouldEqualError(
          futureMaybeResponseJson,
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
      val requestJson = jsonSerializer.serialize(
        JSONRPCRequest(
          jsonrpc = "1.0",
          id = id,
          method = "foo",
          params = ("bar", "baz")
        )
      ).get
      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should respond invalid request") {
        responseShouldEqualError(
          futureMaybeResponseJson,
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
      val requestJson = jsonSerializer.serialize(request).get

      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should respond invalid params") {
        responseShouldEqualError(
          futureMaybeResponseJson,
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
