package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Constants
import io.github.shogowada.scala.jsonrpc.Models.JsonRpcRequest
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import org.scalatest.{AsyncFunSpec, Matchers}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

class FakeJsonSerializer extends JsonSerializer[Writer, Reader] {
  override def serialize[T: Writer](value: T): Option[String] = {
    Option(write[T](value))
  }

  override def deserialize[T: Reader](json: String): Option[T] = {
    Option(read[T](json))
  }
}

class FakeApi {

  import scala.concurrent.ExecutionContext.Implicits.global

  def foo(bar: String, baz: Int): Future[String] = {
    Future(s"$bar$baz")
  }
}

class JsonRpcServerTest extends AsyncFunSpec
    with Matchers {

  override implicit def executionContext = ExecutionContext.Implicits.global

  val jsonSerializer = new FakeJsonSerializer

  val target = JsonRpcServer()

  describe("given I have an API bound") {
    val api = new FakeApi

    target.bindApi(api, jsonSerializer)

    describe("when I received request") {
      val requestId = "request ID"
      val request: JsonRpcRequest[(String, Int)] = JsonRpcRequest(
        jsonrpc = Constants.JsonRpc,
        id = Left(requestId),
        method = classOf[FakeApi].getName + ".foo",
        params = ("bar", 1)
      )
      val requestJson: String = write[JsonRpcRequest[(String, Int)]](request)

      val futureJson = target.receive(requestJson, jsonSerializer)

      it("then it should call the API method") {
        futureJson.map(maybeJson => maybeJson should equal(Option("{}")))
      }
    }
  }
}
