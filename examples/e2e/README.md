# Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP

In this example, we will show how to implement JSON-RPC client on Scala JS and JSON-RPC server on Scala JVM and how to let them communicate via JSON-RPC APIs.

You can see the complete code under this directory, but we have documented some highlights below.

## JSON-RPC API

We define the following 3 JSON-RPC APIs.

```scala
trait CalculatorApi {
  def add(lhs: Int, rhs: Int): Future[Int]
  def subtract(lhs: Int, rhs: Int): Future[Int]
}

trait EchoApi {
  def echo(message: String): Future[String]
}

trait LoggerApi {
  def log(message: String): Unit
}
```

## JSON-RPC server

We implement the APIs on server side like below.

```scala
class CalculatorApiImpl extends CalculatorApi {
  override def add(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs + rhs)
  }
  override def subtract(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs - rhs)
  }
}

class EchoApiImpl extends EchoApi {
  override def echo(message: String): Future[String] = {
    Future(message) // It just returns the message as is
  }
}

class LoggerApiImpl extends LoggerApi {
  override def log(message: String): Unit = {
    println(message) // It logs the message
  }
}
```

We build JSON-RPC server using those API implementations.

```scala
object JsonRpcModule {
  lazy val jsonRpcServer: JsonRpcServer[UpickleJsonSerializer] = {
    val server = JsonRpcServer(UpickleJsonSerializer())
    server.bindApi[CalculatorApi](new CalculatorApiImpl)
    server.bindApi[EchoApi](new EchoApiImpl)
    server.bindApi[LoggerApi](new LoggerApiImpl)
    server
  }
}
```

To expose HTTP end point on the server, we are using [Scalatra](http://www.scalatra.org). We expose POST /jsonrpc end point to receive JSON-RPC request and notification.

```scala
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    context.mount(new JsonRpcServlet, "/jsonrpc/*")
  }
}

class JsonRpcServlet extends ScalatraServlet {
  post("/") {
    val server = JsonRpcModule.jsonRpcServer
    val futureResult: Future[ActionResult] = server.receive(request.body).map {
      case Some(responseJson) => Ok(responseJson) // For JSON-RPC request, we return response.
      case None => NoContent() // For JSON-RPC notification, we do not return response.
    }
    Await.result(futureResult, 1.minutes)
  }
}
```

## JSON-RPC client

On client side, we are using Ajax to send JSON-RPC request and notification. If the server responded 204 (no content), it is JSON-RPC notification.

```scala
val jsonSender: (String) => Future[Option[String]] =
  (json: String) => {
    val NoContentStatus = 204
    dom.ext.Ajax
        .post(url = "/jsonrpc", data = json)
        .map(response => {
          if (response.status == NoContentStatus) {
            None
          } else {
            Option(response.responseText)
          }
        })
  }

val client = JsonRpcClient(UpickleJsonSerializer(), jsonSender)
```

Once the client is built, we can use it to create and use the APIs like below.

```scala
val calculatorApi = client.createApi[CalculatorApi]
val echoApi = client.createApi[EchoApi]
val loggerApi = client.createApi[LoggerApi]

loggerApi.log("This is the beginning of my example.")

calculatorApi.add(1, 2).onComplete {
  case Success(result) => println(s"1 + 2 = $result")
  case _ =>
}

calculatorApi.subtract(1, 2).onComplete {
  case Success(result) => println(s"1 - 2 = $result")
  case _ =>
}

echoApi.echo("Hello, World!").onComplete {
  case Success(result) => println(s"""You said "$result"""")
  case _ =>
}

loggerApi.log("This is the end of my example.")
```

When you run this, you will see that:

- calculations via ```calculatorApi``` is operated on server and returned to client.
- messages sent via ```echoApi``` reaches to server and returned to client as is.
- messages sent via ```loggerApi``` is logged on server.
