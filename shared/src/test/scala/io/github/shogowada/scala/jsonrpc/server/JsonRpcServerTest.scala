package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.models.JsonRpcRequest
import io.github.shogowada.scala.jsonrpc.serializers.JsonSerializer
import org.scalatest.path
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FakeJsonSerializer extends JsonSerializer[Writer, Reader] {
  override def serialize[T: Writer](value: T): Option[String] = {
    Option(write[T](value))
  }

  override def deserialize[T: Reader](json: String): Option[T] = {
    Option(read[T](json))
  }
}

class FakeApi {
  def foo(bar: String, baz: Int): Future[String] = {
    Future(s"$bar$baz")
  }
}

class JsonRpcServerTest extends path.FunSpec {
  override def newInstance: path.FunSpecLike = new JsonRpcServerTest

  val jsonSerializer = new FakeJsonSerializer

  val target = JsonRpcServer()

  describe("given I have an API bound") {
    val api = new FakeApi

    target.bindApi(api)

    describe("when I received request") {
      val requestId = "request ID"
      val request = JsonRpcRequest(
        id = requestId,
        method = "foo",
        params = ("bar", 1)
      )
      val requestJson: String = write(request)

      target.receive(requestJson)(jsonSerializer)
    }
  }
}
