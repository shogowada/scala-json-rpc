# Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket

When your application is both server and client, incoming JSON messages can be either request or response, so you need to make sure requests are received by server and responses are received by client.

You can write something like below to make sure the JSON is handled appropriately.

```scala
val wasJsonRpcResponse: Boolean = jsonRpcClient.receive(json)
if (!wasJsonRpcResponse) {
  jsonRpcServer.receive(json).onComplete {
    case Success(Some(responseJson: String)) => jsonRpcClient.send(responseJson)
    case _ =>
  }
}
```

But it is tedious and error prone to write this everytime you implement bidirectional JSON-RPC.

Because this is such a common use case, we have an API called `JsonRpcServerAndClient` for this purpose, so we recommend using it.

```scala
val serverAndClient = JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
serverAndClient.receive(json)
```

`jsonRpcServerAndClient.receive` makes sure to:

- handle the given JSON using the client first then the server second.
- send the response if present using the client's `send` method.

## Shared

We define the following API for this example:

```scala
trait RandomNumberSubjectApi {
  def register(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit

  def unregister(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit
}
```

Note that we are using `JsonRpcFunction1` to callback the client. For more details about `JsonRpcFunction`, please see [its example page](../jsonRpcFunction).

## Scala JS (browser)

Here is how client communicates with server:

```scala
object Main extends JSApp {
  override def main(): Unit = {
    val webSocket = new dom.WebSocket("ws://localhost:8080/jsonrpc")

    var jsonRpcServerAndClient: JsonRpcServerAndClient[UpickleJsonSerializer] = null

    webSocket.onopen = (_: dom.Event) => {
      jsonRpcServerAndClient = createJsonRpcServerAndClient(webSocket)

      val subjectApi = jsonRpcServerAndClient.createApi[RandomNumberSubjectApi]

      // It can implicitly convert Function1[Int, Unit] to JsonRpcFunction1[Int, Unit].
      subjectApi.register((randomNumber: Int) => {
        println(randomNumber)
        Future() // Making sure server knows if it was successful
      })
    }

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRpcServerAndClient.receive(message)
    }
  }

  private def createJsonRpcServerAndClient(webSocket: WebSocket): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val jsonSerializer = UpickleJsonSerializer()

    val jsonRpcServer = JsonRpcServer(jsonSerializer)

    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(webSocket.send(json)).failed.toOption
          .map(throwable => Future.failed(throwable))
          .getOrElse(Future(None))
    }
    val jsonRpcClient = JsonRpcClient(jsonSerializer, jsonSender)

    JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
  }
}
```

## Server

Here are our API implementation:

```scala
class RandomNumberSubject extends RandomNumberSubjectApi {
  private var registeredObservers: Set[JsonRpcFunction1[Int, Future[Unit]]] = Set()

  def start(): Unit = {
    // Once started, it will generate and notify random numbers to registered observers every second.
    val threadPoolExecutor = new ScheduledThreadPoolExecutor(1)
    val executor = new Runnable {
      override def run() = {
        val randomNumber = (Math.random() * 100.0).toInt
        notifyObservers(randomNumber)
      }
    }
    threadPoolExecutor.scheduleAtFixedRate(executor, 1, 1, TimeUnit.SECONDS)
  }

  private def notifyObservers(randomNumber: Int): Unit = {
    registeredObservers.foreach(observer => {
      observer(randomNumber)
          .failed // Probably the connection is lost
          .foreach(_ => unregister(observer))
    })
  }

  override def register(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit = {
    println(s"Registering observer ${observer.hashCode()}")
    this.synchronized(registeredObservers = registeredObservers + observer)
  }

  override def unregister(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit = {
    println(s"Unregistering observer ${observer.hashCode()}")
    this.synchronized(registeredObservers = registeredObservers - observer)
    observer.dispose()
  }
}
```

Here is our WebSocket implementation:

```scala
class JsonRpcWebSocket extends WebSocketAdapter {
  private var serverAndClient: JsonRpcServerAndClient[UpickleJsonSerializer] = _

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(session.getRemote.sendString(json)).failed.toOption
          .map(throwable => Future.failed(throwable))
          .getOrElse(Future(None))
    }

    // Create an independent server and client for each WebSocket session.
    // This is to make sure we clean up all the caches (e.g. promised response, etc)
    // on each WebSocket session.
    val jsonSerializer = JsonRpcModule.jsonSerializer
    val server = JsonRpcServer(jsonSerializer)
    val client = JsonRpcClient(jsonSerializer, jsonSender)
    serverAndClient = JsonRpcServerAndClient(server, client)

    serverAndClient.bindApi[RandomNumberSubjectApi](JsonRpcModule.randomNumberSubject)
  }

  override def onWebSocketText(message: String): Unit = {
    serverAndClient.receive(message)
  }
}
```
