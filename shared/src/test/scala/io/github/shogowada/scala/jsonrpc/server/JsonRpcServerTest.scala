package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.Constants
import io.github.shogowada.scala.jsonrpc.Models.{JsonRpcRequest, JsonRpcResponse}
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.scalatest.{AsyncFunSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class FakeApi {

  import scala.concurrent.ExecutionContext.Implicits.global

  def foo(bar: String, baz: Int): Future[String] = {
    Future(s"$bar$baz")
  }
}

class JsonRpcServerTest extends AsyncFunSpec
    with Matchers {

  override implicit def executionContext = ExecutionContext.Implicits.global

  describe("given I have an API bound") {
    val api = new FakeApi

    val jsonSerializer = UpickleJsonSerializer()
    val target = JsonRpcServer(jsonSerializer)
        .bindApi(api)

    Seq("foo").foreach(methodName => {
      describe(s"when I received request for method $methodName") {
        val requestId = Left("request ID")
        val request: JsonRpcRequest[(String, Int)] = JsonRpcRequest(
          jsonrpc = Constants.JsonRpc,
          id = requestId,
          method = classOf[FakeApi].getName + s".$methodName",
          params = ("bar", 1)
        )
        val requestJson: String = jsonSerializer.serialize[JsonRpcRequest[(String, Int)]](request).get

        val futureMaybeResponseJson: Future[Option[String]] = target.receive(requestJson)

        it("then it should return the response") {
          val expectedResponse = JsonRpcResponse(jsonrpc = Constants.JsonRpc, id = requestId, result = "bar1")
          futureMaybeResponseJson
              .map(maybeJson => {
                maybeJson.flatMap(json => jsonSerializer.deserialize[JsonRpcResponse[String]](json))
              })
              .map(maybeActualResponse => maybeActualResponse should equal(Some(expectedResponse)))
        }
      }
    })
  }
}
