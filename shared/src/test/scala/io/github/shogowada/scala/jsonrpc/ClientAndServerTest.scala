package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.scalatest.{AsyncFunSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class ClientAndServerTest extends AsyncFunSpec
    with Matchers {

  override implicit def executionContext = ExecutionContext.Implicits.global

  val jsonSerializer = UpickleJsonSerializer()

  var server = JsonRpcServer(jsonSerializer)
  val client: JsonRpcClient[UpickleJsonSerializer] = JsonRpcClient(
    jsonSerializer,
    (json) => {
      server.receive(json)
          .filter((maybeResponseJson: Option[String]) => maybeResponseJson.isDefined)
          .map((maybeResponseJson: Option[String]) => maybeResponseJson.get)
          .foreach((responseJson: String) => client.receive(responseJson))
    }
  )

  trait FakeApi {
    def add(lhs: Int, rhs: Int): Future[Int]

    def subtract(lhs: Int, rhs: Int): Future[Int]
  }

  class FakeApiImpl extends FakeApi {
    override def add(lhs: Int, rhs: Int): Future[Int] = {
      Future(lhs + rhs)
    }

    override def subtract(lhs: Int, rhs: Int): Future[Int] = {
      Future(lhs - rhs)
    }
  }

  server = server.bindApi[FakeApi](new FakeApiImpl)
  val clientApi = client.createApi[FakeApi]

  describe("when I add 2 values") {
    val futureResult = clientApi.add(1, 2)

    it("then it should add the 2 values") {
      futureResult.map(result => result should equal(3))
    }
  }

  describe("when I subtract one value from the other") {
    val futureResult = clientApi.subtract(1, 2)

    it("then it should subtract the value") {
      futureResult.map(result => result should equal(-1))
    }
  }
}
