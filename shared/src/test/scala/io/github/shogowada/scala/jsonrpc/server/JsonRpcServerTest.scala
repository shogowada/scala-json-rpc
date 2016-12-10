package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.communicators.JsonSender
import io.github.shogowada.scala.jsonrpc.models.Models.JsonRpcRequest
import io.github.shogowada.scala.jsonrpc.serializers.{JsonDeserializer, JsonSerializer}
import org.scalatest.path

import scala.collection.mutable
import scala.concurrent.Future

class FakeJsonSender extends JsonSender {
  val sentJsons: mutable.MutableList[String] = mutable.MutableList()

  override def send(json: String): Unit = {
    sentJsons += json
  }
}

class FakeJsonSerializer extends JsonSerializer {
  override def serialize[T](value: T): Option[String] = {
    Option(upickle.default.write(value))
  }
}

class FakeJsonDeserializer extends JsonDeserializer {
  override def deserialize[T](json: String): Option[T] = {
    Option(upickle.default.read(json))
  }
}

class FakeApi {
  def foo(bar: String, baz: Int): Future[String] = {
    Future(s"$bar$baz")
  }
}

class JsonRpcServerTest extends path.FunSpec {
  override def newInstance: path.FunSpecLike = new JsonRpcServerTest

  val jsonSender = new FakeJsonSender
  val jsonSerializer = new FakeJsonSerializer
  val jsonDeserializer = new FakeJsonDeserializer

  val target = JsonRpcServer(
    jsonSender,
    jsonSerializer,
    jsonDeserializer
  )

  describe("given I have an API bound") {
    val api = new FakeApi

    target.bindApi(api)

    describe("when I received request") {
      val requestId = "request ID"
      val request = JsonRpcRequest(
        id = Left(requestId),
        method = "foo",
        params = Seq()
      )
      val requestJson = upickle.default.write(request)

      target.receive(requestJson)
    }
  }
}
