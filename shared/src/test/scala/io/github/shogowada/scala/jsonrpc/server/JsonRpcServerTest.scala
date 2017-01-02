package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Constants
import io.github.shogowada.scala.jsonrpc.Models._
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.scalatest.{Assertion, AsyncFunSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait FakeApi {
  def foo(bar: String, baz: Int): Future[String]

  def notify(message: String): Unit
}

class FakeApiImpl extends FakeApi {

  import scala.concurrent.ExecutionContext.Implicits.global

  val notifiedMessages = ListBuffer.empty[String]

  override def foo(bar: String, baz: Int): Future[String] = {
    Future(s"$bar$baz")
  }

  override def notify(message: String): Unit = {
    notifiedMessages += message
  }
}

class JsonRpcServerTest extends AsyncFunSpec
    with Matchers {

  override implicit def executionContext = ExecutionContext.Implicits.global

  describe("given I have an API bound") {
    val api = new FakeApiImpl

    val jsonSerializer = UpickleJsonSerializer()
    val serverBuilder = JsonRpcServerBuilder(jsonSerializer)
    serverBuilder.bindApi[FakeApi](api)

    val target = serverBuilder.build

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
        expected: JsonRpcErrorResponse[String]
    ): Future[Assertion] = {
      responseShouldEqual(
        futureMaybeJson,
        (json) => jsonSerializer.deserialize[JsonRpcErrorResponse[String]](json),
        expected
      )
    }

    Seq("foo").foreach(methodName => {
      describe(s"when I received request for method $methodName") {
        val requestId = Left("request ID")
        val request: JsonRpcRequest[(String, Int)] = JsonRpcRequest(
          jsonrpc = Constants.JsonRpc,
          id = requestId,
          method = classOf[FakeApi].getName + s".$methodName",
          params = ("bar", 1)
        )
        val requestJson: String = jsonSerializer.serialize(request).get

        val futureMaybeResponseJson: Future[Option[String]] = target.receive(requestJson)

        it("then it should return the response") {
          responseShouldEqual(
            futureMaybeResponseJson,
            (json) => jsonSerializer.deserialize[JsonRpcResultResponse[String]](json),
            JsonRpcResultResponse(
              jsonrpc = Constants.JsonRpc,
              id = requestId,
              result = "bar1"
            )
          )
        }
      }
    })

    describe("when I received notification method") {
      val message = "Hello, World!"
      val notification = JsonRpcNotification[Tuple1[String]](
        jsonrpc = Constants.JsonRpc,
        method = classOf[FakeApi].getName + ".notify",
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
      val request = JsonRpcRequest[(String, String)](
        jsonrpc = Constants.JsonRpc,
        id = id,
        method = "unknown",
        params = ("foo", "bar")
      )
      val requestJson = jsonSerializer.serialize(request).get

      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should respond method not found error") {
        responseShouldEqualError(
          futureMaybeResponseJson,
          JsonRpcErrorResponse(
            jsonrpc = Constants.JsonRpc,
            id = id,
            error = JsonRpcErrors.methodNotFound
          )
        )
      }
    }

    describe("when I receive JSON without method name") {
      val id = Left("id")
      val requestJson = jsonSerializer.serialize(
        JsonRpcRequestId(jsonrpc = Constants.JsonRpc, id = id)
      ).get
      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should respond JSON parse error") {
        responseShouldEqualError(
          futureMaybeResponseJson,
          JsonRpcErrorResponse(
            jsonrpc = Constants.JsonRpc,
            id = id,
            error = JsonRpcErrors.parseError
          )
        )
      }
    }

    describe("when I receive request with mismatching JSON-RPC version") {
      val id = Left("id")
      val requestJson = jsonSerializer.serialize(
        JsonRpcRequest(
          jsonrpc = "1.0",
          id = id,
          method = "foo",
          params = ("bar", "baz")
        )
      ).get
      val futureMaybeResponseJson = target.receive(requestJson)

      it("then it should response invalid request") {
        responseShouldEqualError(
          futureMaybeResponseJson,
          JsonRpcErrorResponse(
            jsonrpc = Constants.JsonRpc,
            id = id,
            error = JsonRpcErrors.invalidRequest
          )
        )
      }
    }
  }
}
