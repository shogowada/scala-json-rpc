# scala-json-rpc

scala-json-rpc is JSON-RPC 2.0 server and client for Scala JVM/JS. It has no dependency and should fit into any of your Scala JVM/JS application.

|Component|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|scala-json-rpc|```"io.github.shogowada" %%% "scala-json-rpc" % "0.1.0"```|2.11, 2.12|0.6|

# Example

In this example, we will implement calculator on server side and call the calculator methods from client side.

## Shared

```scala
// Define an API.
// Note that API methods must return Future type. This is so that client can use the API remotely.
trait CalculatorApi {
  def add(lhs: Int, rhs: Int): Future[Int]
  def subtract(lhs: Int, rhs: Int): Future[Int]
}

// Define how to serialize/deserialize JSON.
class MyJsonSerializer extends JsonSerializer {
  override def serialize[T](value: T): Option[String] = // Serialize model into JSON
  override def deserialize[T](json: String): Option[T] = // Deserialize JSON into model
}
```

## Server side

```scala
// Implement the API.
class CalculatorApiImpl extends CalculatorApi {
  override def add(lhs: Int, rhs: Int): Future[Int] = Future(lhs + rhs)
  override def subtract(lhs: Int, rhs: Int): Future[Int] = Future(lhs - rhs)
}

// Create JSON-RPC server.
// JsonRpcServer is immutable, meaning everytime it binds a new API, it returns a new instance of JsonRpcServer.
// JsonSerializer type parameter is required to support JsonSerializer who's implementation is macro.
val server: JsonRpcServer[MyJsonSerializer] = JsonRpcServer(new MyJsonSerializer())
    .bindApi[CalculatorApi](new CalculatorApiImpl)

// Feed JSON-RPC request into server and send response to client.
val futureMaybeResponse: Future[Option[String]] = server.receive(json)
futureMaybeResponse.onComplete {
  case Success(Some(json)) => // Send the response to client
  case _ =>
}
```

## Client side

```scala
// Create JSON-RPC client.
val jsonSender: (String) => Future[Option[String]] = (json) => {
  // Send JSON to server and return its response as future
}
val client: JsonRpcClient[MyJsonSerializer] = JsonRpcClient(
  new MyJsonSerializer(),
  jsonSender
)

// Create an API.
val calculatorApi: CalculatorApi = client.createApi[CalculatorApi]

// Use the API.
val futureResult: Future[Int] = calculatorApi.add(1, 2)
futureResult.onComplete {
  case Success(result) => println(result)
  case _ =>
}
```

Alternatively, you can feed JSON-RPC responses explicitly like below. You can use whichever flow makes more sense for your application.

```scala
val jsonSender: (String) => Unit = (json) => {
  // Send JSON to server without returning its response as future
}
// ...
client.receive(json) // Explicitly feed JSON-RPC responses
```
