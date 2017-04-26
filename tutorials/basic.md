# Basic

- [Shared code between server and client](#shared-code-between-server-and-client)
    - [Shared contract](#shared-contract)
    - [Shared JSON serialization and deserialization logic](#shared-json-serialization-and-deserialization-logic)
- [JSON-RPC client](#json-rpc-client)
    - [Sending request JSON to server](#sending-request-json-to-server)
    - [Receiving request JSON from server](#receiving-request-json-from-server)
- [JSON-RPC server](#json-rpc-server)
    - [Receiving request JSON from client and sending its response JSON](#receiving-request-json-from-client-and-sending-its-response-json)
- [JSON-RPC server and client](#json-rpc-server-and-client)

## Shared code between server and client

### Shared contract

In scala-json-rpc, your shared contract between server and client is called API. API is a trait defining your methods that you want to call remotely.

For example, if you want to have a repository of `Foo` and make it available remotely, your API might look like this:

```scala
case class Foo(id: String)

trait FooRepositoryAPI {
  def add(foo: Foo): Unit
  def remove(foo: Foo): Unit
  def getAll(): Future[Set[Foo]]
}
```

If your method returns `Future[_]`, it will be JSON-RPC request, meaning you get response from server.

It needs to return `Future[_]` because RPC happens remotely, meaning:

- you will get response in the future.
- your RPC might fail, in which case the `Future[_]` should fail.

If your method returns `Unit`, it will be JSON-RPC notification, meaning you don't get any response including errors. If you don't need anything returned but still want to know if the RPC succeeded, you can let it return `Future[Unit]`.

```scala
trait FooRepositoryAPI {
  def add(foo: Foo): Future[Unit]
  def remove(foo: Foo): Future[Unit]
  def getAll(): Future[Set[Foo]]
}
```

### Shared JSON serialization and deserialization logic

You also want to make sure that your server and client serializes and deserializes JSON in the same way. You can define your JSON serialization and deserialization logic by extending `JsonSerializer` trait.

```scala
class MyJsonSerializer extends JsonSerializer {
  def serialize[T](value: T): Option[String] = {
    // Serialize the value into JSON or return None
    // ...
  }
  def deserialize[T](json: String): Option[T] = {
    // Deserialize the JSON into value or return None
    // ...
  }
}
```

If you just want it to work, you can use `UpickleJsonSerializer` which already implements `JsonSerializer` using [upickle](http://www.lihaoyi.com/upickle-pprint/upickle/) under the hood. See [its README page](/upickle-json-serializer) for details.

If you want to define your own logic, make sure the following requirements are met:

- Serialize/deserialize ```Left[String, BigDecimal]``` as JSON string.
- Serialize/deserialize ```Right[String, BigDecimal]``` as JSON number.
- Serialize/deserialize ```Some[JSONRPCError]``` as [JSON-RPC error object](http://www.jsonrpc.org/specification#error_object).
- Serialize/deserialize ```None[JSONRPCError]``` as JSON null.

## JSON-RPC client

To initiate RPCs, you can use `JSONRPCClient` class. The class's responsibility is to:

- create API client that can initiate RPCs via its methods.
- make sure response is properly returned from corresponding API method call.

For `JSONRPCClient` to work, you need to define:

- how to serialize and deserialize JSON.
    - This is the shared logic we already defined above, so we will skip it here.
- how to send request JSON to server.
- how to receive response JSON from server.

We will cover those in the following sections, but here is a piece of code to give you a general idea of how `JSONRPCClient` works.

```scala
val jsonSerializer = new MyJsonSerializer()
val jsonSender: (String) => Future[Option[String]] = (json: String) => {
  // Send the "json" to server and optionally return its response.
  // We will cover this in the following sections.
  // ...
}
val jsonRPCClient = JSONRPCClient(jsonSerializer, jsonSender)

val fooRepositoryAPI = jsonRPCClient.createAPI[FooRepositoryAPI]

val fooA = Foo("A")
fooRepositoryAPI.add(fooA).onComplete {
  case Success(_) => println(s"Successfully added $fooA to the repository")
  case _ => println(s"Failed to add $fooA to the repository")
}
```

### Sending request JSON to server

Your `JSONRPCClient` needs to know how to send JSON to server so that when you call your API method, it knows how to complete the RPC. You can define the logic as `JsonSender` function.

```scala
type JsonSender = (String) => Future[Option[String]]
```

The function is supposed to:

1. take request JSON as `String`.
2. send the request JSON to server.
3. return optional response from server as `Future[Option[String]]`.
    - Whenever it failed to send the JSON, you need to return failed `Future[Option[String]]` so that it can fail the API method call.

For example, if your server is exposing JSON-RPC endpoint via HTTP at POST /jsonrpc, your `JsonSender` might look like this:

```scala
val jsonSender: (String) => Future[Option[String]] = (json: String) => {
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
```

### Receiving request JSON from server

If you are returning `Future(Some(responseJson))` from your `JsonSender` for all of your requests, it completes the receiving part, so you don't need to worry about this section. This is common if you are using HTTP to send request to server.

If it is not feasible to return response from `JsonSender`, you can let it always return `Future(None)`, and receive it explicitly by using `receive` method of your `JSONRPCClient`. This is common if you are using WebSocket or TCP socket to send request to server.

For example, if you are using WebSocket to send and receive JSON-RPC messages, your code might look like this:

```scala
def start(jsonRPCWebSocketUrl: String): JSONRPCClient[MyJsonSerializer] {
  val webSocket = new dom.WebSocket(jsonRPCWebSocketUrl)

  webSocket.onopen = (_: dom.Event) => {
    val jsonSerializer = UpickleJsonSerializer()
    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(webSocket.send(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }
    val jsonRPCClient = JSONRPCClient(jsonSerializer, jsonSender)

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRPCClient.receive(message)
    }

    // ...
  }
}
```

## JSON-RPC server

To handle RPCs, you can use `JSONRPCServer`. The class's responsibility is to:

- call appropriate API method implementation for each JSON-RPC request and notification.
- return response for each JSON-RPC request.

For `JSONRPCServer` to work, you need to define:

- how to serialize and deserialize JSON.
    - This is the shared logic we already defined above, so we will skip it here.
- how to receive request JSON from client.
- how to send response JSON to client.

We will cover those in the following sections, but here is a piece of code to give you a general idea of how `JSONRPCServer` works.

```scala
val jsonSerializer = new MyJsonSerializer()
val jsonRPCServer = JSONRPCServer(jsonSerializer)

class FooRepositoryAPIImpl extends FooRepositoryAPI {
  // Implement FooRepositoryAPI
  // ...
}

val fooRepositoryAPI = new FooRepositoryAPIImpl
jsonRPCServer.bindAPI[FooRepositoryAPI](fooRepositoryAPI)

jsonRPCServer.receive(String).onComplete {
  case Success(Some(responseJson)) => {
    // RPC is either succeeded or failed, and there is a response for client.
    // We will cover this in the following sections.
    // ...
  }
  case Success(None) => {
    // RPC is succeeded, but there is nothing to respond.
    // This is the case for JSON-RPC notifiation.
  }
  case Failure(throwable) => {
    // Something went wrong, and there was nothing more JSONRPCServer could do.
  }
}
```

### Receiving request JSON from client and sending its response JSON

To receive request JSON from client, you can use `receive` method. The method optionally returns response JSON as `Future[Option[String]]`.

|Return value|Description|
|---|---|
|`Future(Some(responseJson: String))`|Either JSON-RPC request suceeded or failed. `responseJson` can be either JSON-RPC response or error.|
|`Future(None)`|Either JSON-RPC notification succeeded or failed. It has nothing to respond because it was JSON-RPC notification.|
|`Failure(throwable: Throwable)`|None of above.|

If you are using HTTP to take requests, your code might look like this:

```scala
post("/jsonrpc") {
  jsonRPCServer.receive(request.body).map {
    case Some(responseJson: String) => Ok(responseJson) // 200 status
    case None => NoContent() // 204 status
  }
}
```

Or, if you are using WebSocket, it might look like this:

```scala
def onWebSocketText(text: String) {
  jsonRPCServer.receive(text).onComplete {
    case Success(Some(responseJson: String)) => webSocketSession.send(responseJson)
    case Success(None) => // Do nothing
    case Failure(throwable) => // Handle error
  }
}
```

## JSON-RPC server and client

If you want to achieve RPC bidirectionally, you need to have both `JSONRPCServer` and `JSONRPCClient`.

Because this is such a common use case, we also have `JSONRPCServerAndClient`. You can create `JSONRPCServerAndClient` instance by using `JSONRPCServer` and `JSONRPCClient` instances.

```scala
val jsonRPCServer = JSONRPCServer(/* ... */)
val jsonRPCClient = JSONRPCClient(/* ... */)

val jsonRPCServerAndClient = JSONRPCServerAndClient(jsonRPCServer, jsonRPCClient)
```

Just as how it looks like, you think of `JSONRPCServerAndClient` as `JSONRPCServer` and `JSONRPCClient` combined. You can still bind your API implementation using `bindAPI[API](api: API)` method:

```scala
jsonRPCServerAndClient.bindAPI[FooRepositoryAPI](new FooRepositoryAPIImpl)
```

or use `createAPI[API]` to create an API client:

```scala
val fooRepositoryAPI = jsonRPCServerAndClient.createAPI[FooRepositoryAPI]
```

You can receive request JSON and send response JSON by using `receive`. The method can also take care of sending response JSON because it already has access to your `JsonSender` given to `JSONRPCClient`.

```scala
jsonRPCServerAndClient.receive(requestOrResponseJson)
```
