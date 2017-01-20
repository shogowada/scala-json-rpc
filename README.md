# scala-json-rpc

Make communication between your server and client as easy as making function calls!

scala-json-rpc is a [Remove Procedure Call (RPC)](remote procedure call) library for Scala JVM/JS following [JSON-RPC 2.0](http://www.jsonrpc.org/specification) spec. **It has no dependency** and should fit into any of your Scala JVM/JS application.

If you don't know what JSON-RPC is, don't worry. Using this library, you can achieve RPC without knowing much details; all you have to do is to make sure generated string is passed to server and client.

The library has no opinion about how the string should be passed between server and client, so you can achieve RPC over [HTTP](examples/e2e), [WebSocket](examples/e2eWebSocket), TCP socket, or whatever, as long as it is capable of passing strings.

|SBT|Scala Version|Scala JS Version|
|---|---|---|
|```"io.github.shogowada" %%% "scala-json-rpc" % "0.4.2"```|2.12|0.6|

It supports the following features:

- [Send/receive JSON-RPC request](#shared-contract)
- [Send/receive JSON-RPC notification](#shared-contract)
- Respond [standard JSON-RPC error](http://www.jsonrpc.org/specification#error_object)
- [Define custom JSON serialization](/examples/customJsonSerialization)
- [Define custom JSON-RPC method name](/examples/customMethodName)
- [Pass function as parameter](/examples/jsonRpcFunction) :tada:

We have the following example projects for common use cases:

- [Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP](examples/e2e)
- [Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket](examples/e2eWebSocket)

It should already serve you well as a RPC library, but it still does not fully support JSON-RPC spec yet. Here are list of known JSON-RPC features that's not supported yet.

- Send/receive named parameter
    - Define custom parameter name
- Send/receive custom JSON-RPC error
- Define custom JSON-RPC request ID

# Tutorial

[JSON-RPC](http://www.jsonrpc.org) defines a specification of Remote Procedure Call (RPC) in JSON format. This means that you can achieve RPC between your components as long as they are capable of:

- serializing and deserializing JSON.
- passing strings around.

```
+--------+                          +--------+
|        | ---[request as JSON]---> |        |
| Client |                          | Server |
|        | <--[response as JSON]--- |        |
+--------+                          +--------+
```

scala-json-rpc honors the JSON-RPC 2.0 spec, so using scala-json-rpc, you can also achieve RPC as long as you meet the above 2 requirements.

## Shared code between server and client

### Shared contract

In scala-json-rpc, your shared contract between server and client is called API. API is a trait defining your methods that you want to call remotely.

For example, if you want to have a repository of `Foo` and make it available remotely, your API might look like this:

```scala
case class Foo(id: String)

trait FooRepositoryApi {
  def add(foo: Foo): Unit
  def remove(foo: Foo): Unit
  def getAll(): Future[Set[Foo]]
}
```

If your method returns `Future[_]`, it will be JSON-RPC request, meaning you get response from server.

It needs to return `Future[_]` because, by definition, RPC happens remotely, meaning:

- you will get response in the future.
- your RPC might fail, in which case the `Future[_]` should fail.

If your method returns `Unit`, it will be JSON-RPC notification, meaning you don't get any response including errors. If you don't need anything returned but still want to know if the RPC succeeded, you can let it return `Future[Unit]`.

```scala
trait FooRepositoryApi {
  def add(foo: Foo): Future[Unit]
  def remove(foo: Foo): Future[Unit]
  def getAll(): Future[Set[Foo]]
}
```

### Shared JSON serialization and deserialization logic

You also want to make sure that your server and client serializes and deserializes JSON in the same way. You can define your JSON serialization and deserialization logic by extending `JsonSerializer` trait.

```scala
class MyJsonSerializer extends JsonSerializer {
  def serialize[T](value: T): Option[String] = ??? // Serialize the value into JSON or return None
  def deserialize[T](json: String): Option[T] = ??? // Deserialize the JSON into value or return None
}
```

If you just want it to work, you can also use `UpickleJsonSerializer`. See [its README page](/upickle-json-serializer) for details.

## JSON-RPC client

To initiate RPCs, you can use `JsonRpcClient` class. The class's responsibility is to:

- create API client that can initiate RPCs via its methods.
- make sure response is properly returned from corresponding API method call.

For `JsonRpcClient` to work, you need to define:

- how to serialize and deserialize JSON.
    - This is the shared logic we already defined above, so we will skip it here.
- how to send request JSON to server.
- how to receive response JSON from server.

We will cover those in the following sections, but here is a piece of code to give you a general idea of how `JsonRpcClient` works.

```scala
val jsonSerializer = new MyJsonSerializer()
val jsonSender: (String) => Future[Option[String]] = (json: String) => {
  // Send the "json" to server and optionally return its response.
  // We will cover this in the following sections.
  // ...
}
val jsonRpcClient = JsonRpcClient(jsonSerializer, jsonSender)

val fooRepositoryApi = jsonRpcClient.createApi[FooRepositoryApi]

val fooA = Foo("A")
fooRepositoryApi.add(fooA).onComplete {
  case Success(_) => println(s"Successfully added $fooA to the repository")
  case _ => println(s"Failed to add $fooA to the repository")
}
```

### Sending request JSON to server

Your `JsonRpcClient` needs to know how to send JSON to server so that when you call your API method, it knows how to complete the RPC. You can define the logic as `JsonSender` function.

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

If it is not feasible to return response from `JsonSender`, you can let it always return `Future(None)`, and receive it explicitly by using `receive(String)` method of your `JsonRpcClient`. This is common if you are using WebSocket or TCP socket to send request to server.

For example, if you are using WebSocket to send and receive JSON-RPC messages, your code might look like this:

```scala
def start(jsonRpcWebSocketUrl: String): JsonRpcClient[MyJsonSerializer] {
  val webSocket = new dom.WebSocket(jsonRpcWebSocketUrl)

  webSocket.onopen = (_: dom.Event) => {
    val jsonSerializer = UpickleJsonSerializer()
    val jsonSender: (String) => Future[Option[String]] = (json: String) => {
      Try(webSocket.send(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }
    val jsonRpcClient = JsonRpcClient(jsonSerializer, jsonSender)

    webSocket.onmessage = (messageEvent: dom.MessageEvent) => {
      val message = messageEvent.data.toString
      jsonRpcClient.receive(message)
    }
    
    // ...
  }
}
```

## JSON-RPC server

To handle RPCs, you can use `JsonRpcServer`. The class's responsibility is to:

- call appropriate API method implementation for each JSON-RPC request and notification.
- return response for each JSON-RPC request.

For `JsonRpcServer` to work, you need to define:

- how to serialize and deserialize JSON.
    - This is the shared logic we already defined above, so we will skip it here.
- how to receive request JSON from client.
- how to send response JSON to client.

We will cover those in the following sections, but here is a piece of code to give you a general idea of how `JsonRpcServer` works.

```scala
val jsonSerializer = new MyJsonSerializer()
val jsonRpcServer = JsonRpcServer(jsonSerializer)

class FooRepositoryApiImpl extends FooRepositoryApi {
  // Implement FooRepositoryApi
  // ...
}

val fooRepositoryApi = new FooRepositoryApiImpl
jsonRpcServer.bindApi[FooRepositoryApi](fooRepositoryApi)

jsonRpcServer.receive(String).onComplete {
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
    // Something went wrong, and there was nothing more JsonRpcServer could do.
  }
}
```

### Receiving request JSON from client

### Sending response JSON to client

## JSON-RPC server and client
