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

## Scala JS (browser)

```scala
object Main extends JSApp {
  override def main(): Unit = {
    val webSocket = new dom.WebSocket("ws://localhost:8080/jsonrpc")

    val jsonRpcServerAndClient = createJsonRpcServerAndClient(webSocket)

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRpcServerAndClient.receive(message)
    }

    val promisedClientId: Promise[String] = Promise()
    jsonRpcServerAndClient.bindApi[ClientApi](new ClientApiImpl(promisedClientId))
    jsonRpcServerAndClient.bindApi[RandomNumberObserverApi](new RandomNumberObserverApiImpl)

    webSocket.onopen = (_: dom.Event) => {
      val clientIdFactoryApi = jsonRpcServerAndClient.createApi[ClientIdFactoryApi]
      val futureClientId: Future[String] = clientIdFactoryApi.create() // Wait until connection is open to use client APIs

      val subjectApi = jsonRpcServerAndClient.createApi[RandomNumberSubjectApi]

      futureClientId.onComplete {
        case Success(id) => {
          promisedClientId.success(id)
          subjectApi.register(id)
        }
        case _ =>
      }
    }
  }

  private def createJsonRpcServerAndClient(webSocket: WebSocket): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val jsonSerializer = UpickleJsonSerializer()

    val jsonRpcServer = JsonRpcServer(jsonSerializer)

    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(webSocket.send(json))
      Future(None)
    }
    val jsonRpcClient = JsonRpcClient(jsonSerializer, jsonSender)

    JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
  }
}
```
