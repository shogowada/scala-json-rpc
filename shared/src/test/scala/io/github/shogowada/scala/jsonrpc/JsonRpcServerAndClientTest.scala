package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcException
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.scalatest.{AsyncFunSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.Success

class JsonRpcServerAndClientTest extends AsyncFunSpec
    with Matchers {

  val jsonSerializer = UpickleJsonSerializer()

  describe("given I have 2 servers and clients") {
    val server1 = JsonRpcServer(jsonSerializer)
    val server2 = JsonRpcServer(jsonSerializer)

    val client1 = JsonRpcClient(jsonSerializer, (json: String) => server2.receive(json))
    val client2 = JsonRpcClient(jsonSerializer, (json: String) => server1.receive(json))

    val serverAndClient1 = JsonRpcServerAndClient(server1, client1)
    val serverAndClient2 = JsonRpcServerAndClient(server2, client2)

    describe("and I have an API that takes function as parameter") {
      trait Api {
        def foo(bar: JsonRpcFunction[(String) => Future[String]], baz: JsonRpcFunction[(String) => Unit]): Unit
      }

      val barResponses: ListBuffer[String] = ListBuffer()
      val bazValues: ListBuffer[String] = ListBuffer()

      class ApiImpl extends Api {
        var bar: JsonRpcFunction[(String) => Future[String]] = _
        var baz: JsonRpcFunction[(String) => Unit] = _

        override def foo(
            givenBar: JsonRpcFunction[(String) => Future[String]],
            givenBaz: JsonRpcFunction[(String) => Unit]
        ): Unit = {
          bar = givenBar
          bar.call("A").onComplete {
            case Success(response) => barResponses += response
            case _ =>
          }

          baz = givenBaz
          baz.call("B")
        }
      }

      val apiImpl = new ApiImpl
      serverAndClient1.bindApi[Api](apiImpl)

      val apiClient = serverAndClient2.createApi[Api]

      val barImpl: (String) => Future[String] = (bar: String) => Future(bar)
      val bazImpl: (String) => Unit = (baz: String) => bazValues += baz

      it("then it should create a clinet API") {
        apiClient should not be null
      }

      describe("when I call the method") {
        apiClient.foo(barImpl, bazImpl)

        it("then it should send the response for the request function") {
          barResponses.toList should equal(List("A"))
        }

        it("then it should callback the notification function") {
          bazValues.toList should equal(List("B"))
        }

        describe("when I call the functions again") {
          apiImpl.bar.call("C")
          apiImpl.baz.call("D")

          it("then it should call the request function again") {
            barResponses.toList should equal(List("A", "C"))
          }

          it("then it should call the notification function again") {
            bazValues.toList should equal(List("B", "D"))
          }

          describe("but if I dispose the functions") {
            apiImpl.bar.dispose()
            apiImpl.baz.dispose()

            it("then calling the request function should fail") {
              recoverToSucceededIf[JsonRpcException[String]] {
                apiImpl.bar.call("FAKE")
              }
            }

            it("then calling the notification function should ignore the error") {
              noException should be thrownBy apiImpl.baz.call("FAKE")
            }
          }
        }
      }
    }
  }
}
