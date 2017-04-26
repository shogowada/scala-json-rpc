package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Models.JSONRPCException
import io.github.shogowada.scala.jsonrpc.client.JSONRPCClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer
import org.scalatest._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class JSONRPCServerAndClientTest extends AsyncFunSpec
    with OneInstancePerTest
    with Matchers {

  override def newInstance = new JSONRPCServerAndClientTest

  override implicit val executionContext = ExecutionContext.Implicits.global

  val jsonSerializer = UpickleJsonSerializer()

  describe("given I have 2 servers and clients") {
    class TwoServersAndClients {
      val server1 = JSONRPCServer(jsonSerializer)
      val server2 = JSONRPCServer(jsonSerializer)

      val client1 = JSONRPCClient(jsonSerializer, (json: String) => server2.receive(json))
      val client2 = JSONRPCClient(jsonSerializer, (json: String) => server1.receive(json))

      var serverAndClient1 = JSONRPCServerAndClient(server1, client1)
      var serverAndClient2 = JSONRPCServerAndClient(server2, client2)
    }

    describe("and I have an API that takes function as parameter") {
      class AnAPIThatTakesFunction extends TwoServersAndClients {

        trait API {
          def foo(
              requestFunction: DisposableFunction1[String, Future[String]],
              notificationFunction: DisposableFunction1[String, Unit]
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

        class APIImpl extends API {
          var requestFunction: DisposableFunction1[String, Future[String]] = _
          var notificationFunction: DisposableFunction1[String, Unit] = _

          override def foo(
              theRequestFunction: DisposableFunction1[String, Future[String]],
              theNotificationFunction: DisposableFunction1[String, Unit]
          ): Unit = {
            requestFunction = theRequestFunction
            requestFunction(requestValue1).onComplete {
              case Success(response) => promisedRequestResponse1.success(response)
              case _ =>
            }

            notificationFunction = theNotificationFunction
            notificationFunction(notificationValue1)
          }
        }

        val apiImpl = new APIImpl
        serverAndClient1.bindAPI[API](apiImpl)

        val apiClient = serverAndClient2.createAPI[API]

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
      }

      it("then it should create a client API") {
        val fixture = new AnAPIThatTakesFunction
        fixture.apiClient should not be null
      }

      describe("when I call the method") {
        class CallTheMethod extends AnAPIThatTakesFunction {
          apiClient.foo(requestFunctionImpl, notificationFunctionImpl)
        }

        it("then it should send the response for the request function") {
          val fixture = new CallTheMethod
          fixture.promisedRequestResponse1.future
              .map(response => response should equal(fixture.requestValue1))
        }

        it("then it should callback the notification function") {
          val fixture = new CallTheMethod
          fixture.promisedNotificationValue1.future
              .map(value => value should equal(fixture.notificationValue1))
        }

        describe("when I call the functions again") {
          class CallTheFunctionsAgain extends CallTheMethod {
            apiImpl.requestFunction(requestValue2).onComplete {
              case Success(result) => promisedRequestResponse2.success(result)
              case _ =>
            }
            apiImpl.notificationFunction(notificationValue2)
          }

          it("then it should call the request function again") {
            val fixture = new CallTheFunctionsAgain
            fixture.promisedRequestResponse2.future
                .map(response => response should equal(fixture.requestValue2))
          }

          it("then it should call the notification function again") {
            val fixture = new CallTheFunctionsAgain
            fixture.promisedNotificationValue2.future
                .map(value => value should equal(fixture.notificationValue2))
          }

          describe("but if I dispose the functions") {
            class DisposeTheFunction extends CallTheFunctionsAgain {
              val futureDisposeRequestFunctionResult = apiImpl.requestFunction.dispose()
              val futureDisposeNotificationFunctionResult = apiImpl.notificationFunction.dispose()
            }

            it("then it should successfully dispose the request function") {
              val fixture = new DisposeTheFunction
              fixture.futureDisposeRequestFunctionResult
                  .map(result => result should equal(()))
            }

            it("then it should successfully dispose the notification function") {
              val fixture = new DisposeTheFunction
              fixture.futureDisposeNotificationFunctionResult
                  .map(result => result should equal(()))
            }

            it("then calling the request function should fail") {
              val fixture = new DisposeTheFunction
              fixture.apiImpl.requestFunction("FAKE").failed
                  .map(throwable => throwable.isInstanceOf[JSONRPCException[_]] should equal(true))
            }

            it("then calling the notification function should ignore the error") {
              val fixture = new DisposeTheFunction
              noException should be thrownBy fixture.apiImpl.notificationFunction("FAKE")
            }
          }
        }
      }
    }

    describe("given I have an API that returns a function") {
      class APIThatReturnsFunction extends TwoServersAndClients {

        trait API {
          def foo: Future[DisposableFunction0[Future[String]]]
        }

        class APIImpl extends API {
          override def foo = Future(() => Future("foo"))
        }

        serverAndClient1.bindAPI[API](new APIImpl)
        val client = serverAndClient2.createAPI[API]
      }

      describe("when I call the function") {
        class CallTheFunction extends APIThatReturnsFunction {
          val futureFunction: Future[DisposableFunction0[Future[String]]] = client.foo
          val futureFoo: Future[String] = futureFunction.flatMap(function => function())
        }

        it("then it should execute the function") {
          val fixture = new CallTheFunction
          fixture.futureFoo
              .map(foo => foo should equal("foo"))
        }

        describe("when I dispose the function") {
          class DisposeTheFunction extends CallTheFunction {
            val futureDisposeAcknowledgement: Future[Unit] = futureFunction.flatMap(function => function.dispose())
          }

          it("then it should succeed") {
            val fixture = new DisposeTheFunction
            fixture.futureDisposeAcknowledgement
                .map(_ => succeed)
          }

          describe("and I try to use the function") {
            class TryToUseTheFunction extends DisposeTheFunction {
              val futureFooAfterDisposal: Future[String] = futureDisposeAcknowledgement
                  .flatMap(_ => futureFunction)
                  .flatMap(function => function())
            }

            it("then it should fail") {
              val fixture = new TryToUseTheFunction
              fixture.futureFooAfterDisposal
                  .failed
                  .map(_ => succeed)
            }
          }
        }
      }
    }

    describe("given I have an API that takes the same function type in 2 places") {
      class ClientAPIThatTakes2Functions extends TwoServersAndClients {
        val promisedFoo1Function: Promise[DisposableFunction0[Unit]] = Promise()
        val promisedFoo2Function: Promise[DisposableFunction0[Unit]] = Promise()

        trait API {
          def foo1(bar: DisposableFunction0[Unit]): Unit

          def foo2(bar: DisposableFunction0[Unit]): Unit
        }

        class APIImpl extends API {
          override def foo1(bar: DisposableFunction0[Unit]): Unit = {
            promisedFoo1Function.success(bar)
          }

          override def foo2(bar: DisposableFunction0[Unit]): Unit = {
            promisedFoo2Function.success(bar)
          }
        }

        serverAndClient1.bindAPI[API](new APIImpl)
        val client = serverAndClient2.createAPI[API]
      }

      describe("when I call them both with the same function") {
        class CallThemBothWithTheSameFunction extends ClientAPIThatTakes2Functions {
          val function: () => Unit = () => {}
          client.foo1(function)
          client.foo2(function)
        }

        it("then it should use the same function reference on the server too") {
          val fixture = new CallThemBothWithTheSameFunction
          for {
            foo1Function <- fixture.promisedFoo1Function.future
            foo2Function <- fixture.promisedFoo2Function.future
          } yield foo1Function should be(foo2Function)
        }
      }

      describe("when I call them both with different functions") {
        class CallThemBothWithDifferentFunctions extends ClientAPIThatTakes2Functions {
          client.foo1(() => {})
          client.foo2(() => {})
        }

        it("then it should use different function references on the server too") {
          val fixture = new CallThemBothWithDifferentFunctions
          for {
            foo1Function <- fixture.promisedFoo1Function.future
            foo2Function <- fixture.promisedFoo2Function.future
          } yield foo1Function should not be foo2Function
        }
      }
    }

    describe("and I changed the reference to servers and clients") {
      class IChangedTheReference extends TwoServersAndClients {

        trait API {
          def foo(): Future[String]
        }

        class APIImpl extends API {
          override def foo(): Future[String] = {
            Future("foo")
          }
        }

        serverAndClient1.bindAPI[API](new APIImpl)
        val api = serverAndClient2.createAPI[API]

        serverAndClient1 = null
        serverAndClient2 = null
      }

      it("but the APIs should still work") {
        val fixture = new IChangedTheReference
        fixture.api.foo().map(result => result should equal("foo"))
      }
    }
  }
}
