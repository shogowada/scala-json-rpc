# scala-json-rpc

scala-json-rpc is JSON-RPC 2.0 server and client for Scala JVM/JS. **It has no dependency** and should fit into any of your Scala JVM/JS application.

|Component|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|scala-json-rpc|```"io.github.shogowada" %%% "scala-json-rpc" % "0.2.3"```|2.11, 2.12|0.6|
|[scala-json-rpc-upickle-json-serializer](/upickle-json-serializer)|```"io.github.shogowada" %%% "scala-json-rpc-upickle-json-serializer" % "0.2.3"```|2.11, 2.12|0.6|

It supports the following features:

- Send/receive JSON-RPC request
- [Send/receive JSON-RPC notification](/examples/notification)
- [Respond pre-defined JSON-RPC error](http://www.jsonrpc.org/specification#error_object)
- [Define custom JSON serialization](/examples/customJsonSerialization)
- [Define custom JSON-RPC method name](/examples/customMethodName)

We have the following example projects for common use cases:

- [Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP](examples/e2e)
- Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket (coming soon)

It should already serve you well as a RPC library, but it still does not fully support JSON-RPC spec yet. Here are list of known JSON-RPC features that's not supported yet.

- Send/receive named parameter
    - Define custom parameter name
- Send/receive custom JSON-RPC error
- Define custom JSON-RPC request ID

# Quick Look

In this example, we will implement calculator on server side and call the calculator methods from client side.

## Shared

```scala
// Define an API.
// Note that API methods must return either Future or Unit type.
// If the method returns Future, it will be JSON-RPC request method, and client can receive response.
// If the method returns Unit, it will be JSON-RPC notification method, and client does not receive response.
trait CalculatorApi {
  def add(lhs: Int, rhs: Int): Future[Int]
  def subtract(lhs: Int, rhs: Int): Future[Int]
}

// Define how to serialize/deserialize JSON.
class MyJsonSerializer extends JsonSerializer {
  override def serialize[T](value: T): Option[String] = // ... Serialize model into JSON.
  override def deserialize[T](json: String): Option[T] = // ... Deserialize JSON into model.
}
```

You can also use [upickle-json-serializer](/upickle-json-serializer) as your ```JsonSerializer``` instead of [implementing it by yourself](/examples/customJsonSerialization).

## Server side

```scala
// Implement the API.
class CalculatorApiImpl extends CalculatorApi {
  override def add(lhs: Int, rhs: Int): Future[Int] = Future(lhs + rhs)
  override def subtract(lhs: Int, rhs: Int): Future[Int] = Future(lhs - rhs)
}

// Create JSON-RPC server.
// JsonSerializer type parameter is required to support JsonSerializer who's implementation is macro (e.g. upickle).
// You can bind as many APIs as you want.
val serverBuilder = JsonRpcServerBuilder[MyJsonSerializer](new MyJsonSerializer())
serverBuilder.bindApi[CalculatorApi](new CalculatorApiImpl)

val server: JsonRpcServer[MyJsonSerializer] = serverBuilder.build

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
val clientBuilder = JsonRpcClientBuilder[MyJsonSerializer](
  new MyJsonSerializer(),
  jsonSender
)
val client: JsonRpcClient[MyJsonSerializer] = clientBuilder.build

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

Alternatively, you can feed JSON-RPC responses explicitly like below. You can use whichever flow makes more sense for your application. For example, if you are using web socket to connect client and server, this flow might make more sense than to return ```Future[Option[String]]``` from the JSON sender.

```scala
val jsonSender: (String) => Unit = (requestJson) => {
  // Send JSON to server without returning its response as future.
  // Because client doesn't have access to the response now, you need to explicitly feed the response like below.
  // ...
}
// ...
client.receive(responseJson) // Explicitly feed JSON-RPC responses.
```
