package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Models.JsonRpcException
import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer
import org.scalatest.{AsyncFunSpec, Matchers}

import scala.concurrent.{Future, Promise}
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

      val promisedA1: Promise[String] = Promise()
      val promisedB1: Promise[String] = Promise()
      val promisedB2: Promise[String] = Promise()

      class ApiImpl extends Api {
        var bar: JsonRpcFunction[(String) => Future[String]] = _
        var baz: JsonRpcFunction[(String) => Unit] = _

        override def foo(
            theBar: JsonRpcFunction[(String) => Future[String]],
            theBaz: JsonRpcFunction[(String) => Unit]
        ): Unit = {
          bar = theBar
          bar.call("A1").onComplete {
            case Success(response) => promisedA1.success(response)
            case _ =>
          }

          baz = theBaz
          baz.call("B1")
        }
      }

      val apiImpl = new ApiImpl
      serverAndClient1.bindApi[Api](apiImpl)

      val apiClient = serverAndClient2.createApi[Api]

      val barImpl: (String) => Future[String] = (bar: String) => {
        Future(bar)
      }
      val bazImpl: (String) => Unit = (baz: String) => {
        baz match {
          case "B1" => promisedB1.success(baz)
          case "B2" => promisedB2.success(baz)
          case _ =>
        }
      }

      it("then it should create a clinet API") {
        apiClient should not be null
      }

      describe("when I call the method") {
        apiClient.foo(barImpl, bazImpl)

        it("then it should send the response for the request function") {
          promisedA1.future.map(a1 => a1 should equal("A1"))
        }

        it("then it should callback the notification function") {
          promisedB1.future.map(b1 => b1 should equal("B1"))
        }

        describe("when I call the functions again") {
          val promisedA2: Promise[String] = Promise()

          apiImpl.bar.call("A2").onComplete {
            case Success(result) => promisedA2.success(result)
            case _ =>
          }

          apiImpl.baz.call("B2")

          it("then it should call the request function again") {
            promisedA2.future.map(a2 => a2 should equal("A2"))
          }

          it("then it should call the notification function again") {
            promisedB2.future.map(b2 => b2 should equal("B2"))
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
