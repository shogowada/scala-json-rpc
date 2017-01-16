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
        def foo(
            requestFunction: JsonRpcFunction[(String) => Future[String]],
            notificationFunction: JsonRpcFunction[(String) => Unit]
        ): Unit
      }

      val requestValue1 = "A1"
      val requestValue2 = "A2"
      val notificationValue1 = "B1"
      val notificationValue2 = "B2"

      val promisedRequestResponse1: Promise[String] = Promise()
      val promisedRequestResponse2: Promise[String] = Promise()
      val promisedNotificationValue1: Promise[String] = Promise()
      val promisedNotificationValue2: Promise[String] = Promise()

      class ApiImpl extends Api {
        var requestFunction: JsonRpcFunction[(String) => Future[String]] = _
        var notificationFunction: JsonRpcFunction[(String) => Unit] = _

        override def foo(
            theRequestFunction: JsonRpcFunction[(String) => Future[String]],
            theNotificationFunction: JsonRpcFunction[(String) => Unit]
        ): Unit = {
          requestFunction = theRequestFunction
          requestFunction.call(requestValue1).onComplete {
            case Success(response) => promisedRequestResponse1.success(response)
            case _ =>
          }

          notificationFunction = theNotificationFunction
          notificationFunction.call(notificationValue1)
        }
      }

      val apiImpl = new ApiImpl
      serverAndClient1.bindApi[Api](apiImpl)

      val apiClient = serverAndClient2.createApi[Api]

      val requestFunctionImpl: (String) => Future[String] = (value: String) => {
        Future(value)
      }
      val notificationFunctionImpl: (String) => Unit = (value: String) => {
        value match {
          case `notificationValue1` => promisedNotificationValue1.success(value)
          case `notificationValue2` => promisedNotificationValue2.success(value)
          case _ =>
        }
      }

      it("then it should create a clinet API") {
        apiClient should not be null
      }

      describe("when I call the method") {
        apiClient.foo(requestFunctionImpl, notificationFunctionImpl)

        it("then it should send the response for the request function") {
          promisedRequestResponse1.future
              .map(response => response should equal(requestValue1))
        }

        it("then it should callback the notification function") {
          promisedNotificationValue1.future
              .map(value => value should equal(notificationValue1))
        }

        describe("when I call the functions again") {
          apiImpl.requestFunction.call(requestValue2).onComplete {
            case Success(result) => promisedRequestResponse2.success(result)
            case _ =>
          }

          apiImpl.notificationFunction.call(notificationValue2)

          it("then it should call the request function again") {
            promisedRequestResponse2.future
                .map(response => response should equal(requestValue2))
          }

          it("then it should call the notification function again") {
            promisedNotificationValue2.future
                .map(value => value should equal(notificationValue2))
          }

          describe("but if I dispose the functions") {
            val futureDisposeRequestFunctionResult = apiImpl.requestFunction.dispose()
            val futureDisposeNotificationFunctionResult = apiImpl.notificationFunction.dispose()

            it("then it should successfully dispose the request function") {
              futureDisposeRequestFunctionResult
                  .map(result => result should equal(()))
            }

            it("then it should successfully dispose the notification function") {
              futureDisposeNotificationFunctionResult
                  .map(result => result should equal(()))
            }

            it("then calling the request function should fail") {
              recoverToSucceededIf[JsonRpcException[String]] {
                apiImpl.requestFunction.call("FAKE")
              }
            }

            it("then calling the notification function should ignore the error") {
              noException should be thrownBy apiImpl.notificationFunction.call("FAKE")
            }
          }
        }
      }
    }
  }
}
