package io.github.shogowada.scala.jsonrpc

import io.github.shogowada.scala.jsonrpc.Models.JSONRPCException
import io.github.shogowada.scala.jsonrpc.client.JSONRPCClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJSONSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class JSONRPCServerAndClientTest extends BaseSpec {

  override def newInstance = new JSONRPCServerAndClientTest

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val jsonSerializer = UpickleJSONSerializer()

  "given I have 2 servers and clients" - {
    val server1 = JSONRPCServer(jsonSerializer)
    val server2 = JSONRPCServer(jsonSerializer)

    val client1 = JSONRPCClient(jsonSerializer, (json: String) => server2.receive(json))
    val client2 = JSONRPCClient(jsonSerializer, (json: String) => server1.receive(json))

    var serverAndClient1 = JSONRPCServerAndClient(server1, client1)
    var serverAndClient2 = JSONRPCServerAndClient(server2, client2)

    "and I have an API that takes function as parameter" - {
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
      val notificationFunctionImpl: (String) => Unit = {
        case `notificationValue1` => promisedNotificationValue1.success(notificationValue1)
        case `notificationValue2` => promisedNotificationValue2.success(notificationValue2)
        case _ =>
      }

      "then it should create a client API" in {
        apiClient should not be null
      }

      "when I call the method" - {
        apiClient.foo(requestFunctionImpl, notificationFunctionImpl)

        "then it should send the response for the request function" in {
          promisedRequestResponse1.future
              .map(response => response should equal(requestValue1))
        }

        "then it should callback the notification function" in {
          promisedNotificationValue1.future
              .map(value => value should equal(notificationValue1))
        }

        "when I call the functions again" - {
          apiImpl.requestFunction(requestValue2).onComplete {
            case Success(result) => promisedRequestResponse2.success(result)
            case _ =>
          }
          apiImpl.notificationFunction(notificationValue2)

          "then it should call the request function again" in {
            promisedRequestResponse2.future
                .map(response => response should equal(requestValue2))
          }

          "then it should call the notification function again" in {
            promisedNotificationValue2.future
                .map(value => value should equal(notificationValue2))
          }

          "but if I dispose the functions" - {
            val futureDisposeRequestFunctionResult = apiImpl.requestFunction.dispose()
            val futureDisposeNotificationFunctionResult = apiImpl.notificationFunction.dispose()

            "then it should successfully dispose the request function" in {
              futureDisposeRequestFunctionResult
                  .map(result => result should equal(()))
            }

            "then it should successfully dispose the notification function" in {
              futureDisposeNotificationFunctionResult
                  .map(result => result should equal(()))
            }

            "then calling the notification function should ignore the error" in {
              noException should be thrownBy apiImpl.notificationFunction("FAKE")
            }

            "and calling the request function" - {
              val actual = apiImpl.requestFunction("FAKE").failed

              "should fail" in {
                actual
                    .map(throwable => throwable.isInstanceOf[JSONRPCException[_]] should equal(true))
              }
            }
          }
        }
      }
    }

    "given I have an API that returns a function" - {
      trait API {
        def foo: Future[DisposableFunction0[Future[String]]]
      }

      class APIImpl extends API {
        override def foo = Future(() => Future("foo"))
      }

      serverAndClient1.bindAPI[API](new APIImpl)
      val client = serverAndClient2.createAPI[API]

      "calling the function" - {
        val futureFunction: Future[DisposableFunction0[Future[String]]] = client.foo
        val futureFoo: Future[String] = futureFunction.flatMap(function => function())

        "should execute the function" in {
          futureFoo.map(foo => foo should equal("foo"))
        }

        "and disposing the function" - {
          val futureDisposeAcknowledgement: Future[Unit] = futureFoo
              .flatMap(_ => futureFunction)
              .flatMap(function => function.dispose())

          "should succeed" in {
            futureDisposeAcknowledgement.map(_ => succeed)
          }

          "and using the disposed function" - {
            val futureFooAfterDisposal: Future[String] = futureDisposeAcknowledgement
                .flatMap(_ => futureFunction)
                .flatMap(function => function())

            "should fail" in {
              futureFooAfterDisposal
                  .failed
                  .map(_ => succeed)
            }
          }
        }
      }
    }

//   "given I have an API that returns either integer or function" - {
//      trait API {
//        def foo(): Future[Either[Int, DisposableFunction0[Unit]]]
//      }
//
//      val called: Promise[Unit] = Promise()
//
//      val apiServer = new API {
//        override def foo(): Future[Either[Int, DisposableFunction0[Unit]]] = Future {
//          Right(DisposableFunction(() =>
//            called.success(())
//          ))
//        }
//      }
//
//      serverAndClient1.bindAPI[API](apiServer)
//      val api = serverAndClient2.createAPI[API]
//
//      "calling the returned function" - {
//        val futureFunction = api.foo().map {
//          case Right(function) => function
//          case _ => fail("Expected the result to be right")
//        }
//        futureFunction.foreach(_.apply())
//
//        "should actually call the function" in {
//          called.future.map(_ => succeed)
//        }
//      }
//    }

    "given I have an API that takes the same function type in 2 places" - {
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

      "calling them both with the same function should use the same function reference on the server too" in {
        val function: () => Unit = () => {}
        client.foo1(function)
        client.foo2(function)

        for {
          foo1Function <- promisedFoo1Function.future
          foo2Function <- promisedFoo2Function.future
        } yield foo1Function should be(foo2Function)
      }

      "calling them both with different functions should use different function references on the server too" in {
        client.foo1(() => {})
        client.foo2(() => {})

        for {
          foo1Function <- promisedFoo1Function.future
          foo2Function <- promisedFoo2Function.future
        } yield foo1Function should not be foo2Function
      }
    }

    "and I changed the reference to servers and clients" - {
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

      "but the APIs should still work" in {
        api.foo().map(result => result should equal("foo"))
      }
    }
  }
}
