# scala-json-rpc

Make communication between your server and client as easy as making function calls!

scala-json-rpc is a [Remove Procedure Call (RPC)](remote procedure call) library for Scala JVM/JS following [JSON-RPC 2.0](http://www.jsonrpc.org/specification) spec. **It has no dependency** and should fit into any of your Scala JVM/JS application.

If you don't know what JSON-RPC is, don't worry. Using this library, you can achieve RPC without knowing much details; all you have to do is to make sure generated string is passed to server and client.

The library has no opinion about how the string should be passed between server and client, so you can achieve RPC over [HTTP](examples/e2e), [WebSocket](examples/e2eWebSocket), TCP socket, or whatever, as long as it is capable of passing strings.

|Component|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|scala-json-rpc|```"io.github.shogowada" %%% "scala-json-rpc" % "0.3.0"```|2.11, 2.12|0.6|
|[scala-json-rpc-upickle-json-serializer](/upickle-json-serializer)|```"io.github.shogowada" %%% "scala-json-rpc-upickle-json-serializer" % "0.3.0"```|2.11, 2.12|0.6|

It supports the following features:

- Send/receive JSON-RPC request
- [Send/receive JSON-RPC notification](/examples/notification)
- Respond [standard JSON-RPC error](http://www.jsonrpc.org/specification#error_object)
- [Define custom JSON serialization](/examples/customJsonSerialization)
- [Define custom JSON-RPC method name](/examples/customMethodName)

We have the following example projects for common use cases:

- [Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP](examples/e2e)
- [Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket](examples/e2eWebSocket)

It should already serve you well as a RPC library, but it still does not fully support JSON-RPC spec yet. Here are list of known JSON-RPC features that's not supported yet.

- Send/receive named parameter
    - Define custom parameter name
- Send/receive custom JSON-RPC error
- Define custom JSON-RPC request ID

# Quick Look

In this example, we will implement calculator on server side and call the calculator methods from client side.

## Shared

### Define an API

```scala
// Note that API methods must return either Future or Unit type.
// If the method returns Future, it will be JSON-RPC request method, and client can receive response.
// If the method returns Unit, it will be JSON-RPC notification method, and client does not receive response.
trait CalculatorApi {
  def add(lhs: Int, rhs: Int): Future[Int]
  def subtract(lhs: Int, rhs: Int): Future[Int]
}
```

### Define JSON serialization/deserialization method

If you just want it to work, you can also use [upickle-json-serializer](/upickle-json-serializer) as your ```JsonSerializer``` instead of [implementing it by yourself](/examples/customJsonSerialization).

```scala
class MyJsonSerializer extends JsonSerializer {
  override def serialize[T](value: T): Option[String] = // ... Serialize model into JSON.
  override def deserialize[T](json: String): Option[T] = // ... Deserialize JSON into model.
}

val jsonSerializer = new MyJsonSerializer()

// Or

val jsonSerializer = UpickleJsonSerializer()
```

## Server side

```scala
// Implement the API.
class CalculatorApiImpl extends CalculatorApi {
  override def add(lhs: Int, rhs: Int): Future[Int] = Future(lhs + rhs)
  override def subtract(lhs: Int, rhs: Int): Future[Int] = Future(lhs - rhs)
}

// Create JSON-RPC server.
// You can bind as many APIs as you want.
val server = JsonRpcServer(new MyJsonSerializer())
server.bindApi[CalculatorApi](new CalculatorApiImpl)

// Feed JSON-RPC request into server and send its response to client.
// Server's receive method returns Future[Option[String]], where the String is JSON-RPC response.
// If the response is present, you are supposed to send it back to client.
val requestJson: String = // ... JSON-RPC request as JSON
val futureMaybeResponse: Future[Option[String]] = server.receive(requestJson)
futureMaybeResponse.onComplete {
  case Success(Some(responseJson)) => // Send the response to client.
  case Success(None) => // Response is absent if it was JSON-RPC notification.
  case _ =>
}
```

## Client side

```scala
// Create JSON-RPC client.
val jsonSender: (String) => Future[Option[String]] = (requestJson) => {
  // By returning the future here, it will automatically take care of the responses for you.
  val futureMaybeResponseJson: Future[Option[String]] = // ... Send the request JSON and receive its response.
  futureMaybeResponseJson
}
val client = JsonRpcClient(
  new MyJsonSerializer(),
  jsonSender
)

// Create an API.
// You can create as many APIs as you want.
val calculatorApi: CalculatorApi = client.createApi[CalculatorApi]

// Use the API.
// When you invoke an API method, under the hood, it:
// 1. creates a JSON-RPC request with the given parameters.
// 2. serializes the request into JSON using JsonSerializer.
// 3. sends the JSON to server using JsonSender.
// 4. receives the response JSON via Future[Option[String]] returned from the JsonSender.
//     - Or, it receives the response JSON via client.receive(responseJson) method.
// 5. deserializes the response JSON into JSON-RPC response using JsonSerializer.
// 6. completes the Future returned by the API method with result of the JSON-RPC response.
val futureResult: Future[Int] = calculatorApi.add(1, 2)
futureResult.onComplete {
  case Success(result) => // ... Do something with the result.
  case _ =>
}
```

Alternatively, you can feed JSON-RPC responses explicitly like below. You can use whichever flow makes more sense for your application. For example, if you are using WebSocket to connect client and server, this flow might make more sense than to return ```Future[Option[String]]``` from the JSON sender.

```scala
val jsonSender: (String) => Future[Option[String]] = (requestJson) => {
  // If client doesn't have access to the future response now, you can explicitly feed the response like below too.
  // ...
  Future(None)
}
// ...
client.receive(responseJson) // Explicitly feed JSON-RPC responses.
```
