# scala-json-rpc

scala-json-rpc is JSON-RPC 2.0 server and client for Scala JVM/JS. **It has no dependency** and should fit into any of your Scala JVM/JS application.

|Component|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|scala-json-rpc|```"io.github.shogowada" %%% "scala-json-rpc" % "0.2.0"```|2.11, 2.12|0.6|

It should already serve you well as RMI library, but it is still in early development and does not fully support JSON-RPC spec yet. Here are list of known features that's not supported yet.

- Custom method name
- Named parameter
    - Custom parameter name
- Custom JSON-RPC error
- Custom JSON-RPC request ID

# Example

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

- For now, JSON-RPC method names are full name for the API method. For example, if the above ```CalculatorApi``` is defined at package ```io.github.shogowada```, then JSON-RPC method name for the ```add``` method will be ```io.github.shogowada.CalculatorApi.add```.
    - This is convenient if your are using this library only to achieve RMI between your Scala components. In the future, you will be able to name your methods.

## Server side

```scala
// Implement the API.
class CalculatorApiImpl extends CalculatorApi {
  override def add(lhs: Int, rhs: Int): Future[Int] = Future(lhs + rhs)
  override def subtract(lhs: Int, rhs: Int): Future[Int] = Future(lhs - rhs)
}

// Create JSON-RPC server.
// JsonSerializer type parameter is required to support JsonSerializer who's implementation is macro.
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
  // Send JSON to server and return its response as future.
  // By returning the future here, it will automatically take care of the responses for you.
  // ...
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
// When you invoke an API method,
//     1. It creates a JSON-RPC request.
//     2. It serializes the request into JSON using JsonSerializer.
//     3. It sends the JSON to server using JsonSender.
//     4. It receives the response JSON via Future[Option[String]] returned from the JsonSender.
//     5. It deserializes the response JSON into JSON-RPC response using JsonSerializer.
//     6. It completes the Future returned by the API method with result of the JSON-RPC response.
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
  // Because client doesn't have access to the response, you need to explicitly feed the response like below.
  // ...
}
// ...
client.receive(json) // Explicitly feed JSON-RPC responses.
```

### Other Examples
- [JSON-RPC notification](/examples/notification)
