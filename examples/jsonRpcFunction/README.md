# Passing function as parameter

You can pass functions as parameter by using `JsonRpcFunction` with `JsonRpcServerAndClient`. This is useful if you want callbacks from server.

- [Create JsonRpcServerAndClient](#create-jsonrpcserverandclient)
- [Create API that takes JsonRpcFunction as parameter](#create-api-that-takes-jsonrpcfunction-as-parameter)
- [Implement server](#implement-server)
- [Implement client](#implement-client)
- [But how is it working? I thought it's JSON-RPC library!](#but-how-is-it-working-i-thought-its-json-rpc-library)
- [End to end example over WebSocket](#end-to-end-example-over-websocket)
- [Summary](#summary)

## Create JsonRpcServerAndClient

To create `JsonRpcServerAndClient`, you need to create a server and a client first like you normally would.

```scala
val jsonSerializer = new MyJsonSerializer()
val jsonSender: (String) => Future[Option[String]] = {
  // ... Implement JSON sender
}
val jsonRpcServer: JsonRpcServer[MyJsonSerializer] = JsonRpcServer(jsonSerializer)
val jsonRpcClient: JsonRpcClient[MyJsonSerializer] = JsonRpcClient(jsonSerializer, jsonSender)
```

Then, you can create `JsonRpcServerAndClient` using the server and the client.

```scala
val jsonRpcServerAndClient: JsonRpcServerAndClient[MyJsonSerializer] =
    JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
```

## Create API that takes JsonRpcFunction as parameter

```scala
trait EchoApi {
  def echo(message: String, callback: JsonRpcFunction1[String, Unit]): Unit
}

trait UuidSubjectApi {
  def register(observer: JsonRpcFunction1[String, Future[Unit]]): Unit
  def unregister(observer: JsonRpcFunction1[String, Future[Unit]]): Unit
}
```

Internally, `JsonRpcFuncion` is just another JSON-RPC client, so just like an API method, **you need to return either `Unit` or `Future`**.

The `UuidSubjectApi.unregister` works because **if the same function reference is used on client side, it will be the same function reference on server side too**.

## Implement server

```scala
class EchoApiImpl extends EchoApi {
  override def echo(message: String, callback: JsonRpcFunction1[String, Unit]): Unit = {
    callback(message)
    callback.dispose() // Dispose the function when you no longer need it
  }
}

class UuidSubjectApiImpl extends UuidSubjectApi {
  var observers: Set[JsonRpcFunction1[String, Future[Unit]]] = Set()
  
  // ... Set timer so that we will invoke the following method periodically.
  def notify() {
    val uuid = UUID.randomUUID().toString
    observers.foreach(observer => {
      observer(uuid)
          .failed // Probably the connection is lost
          .foreach(_ => unregister(observer))
    })
  }

  override def register(observer: JsonRpcFunction1[String, Future[Unit]]): Unit = this.synchronized {
    observers = observers + observer
  }
  
  override def unregister(observer: JsonRpcFunction1[String, Future[Unit]]): Unit = this.synchronized {
    observers = observers - observer
    observer.dispose() // We no longer need it.
  }
}

val serverAndClient = JsonRpcServerAndClient(/* ... */)
serverAndClient.bindApi[EchoApi](new EchoApiImpl)
serverAndClient.bindApi[UuidSubjectApi](new UuidSubjectApiImpl)
```

You can use the `JsonRpcFunction` just like regular function except **you need to explicitly dispose the function when you no longer need it**. This is so that both server and client can dispose relative mappings.

## Implement client

```scala
val serverAndClient = JsonRpcServerAndClient(/* ... */)

val echoApi = serverAndClient.createApi[Api]
echoApi.echo("Hello, World!", (message: String) => {
  println(s"Server echoed: $message")
})

val uuidObserver: (String) => Future[Unit] = (uuid: String) => {
  println(s"Notified UUID: $uuid")
  Future() // Let server know it was successful
}
val uuidSubjectApi = serverAndClient.createApi[UuidSubjectApi]
uuidSubjectApi.register(uuidObserver)
uuidSubjectApi.unregister(uuidObserver)
```

On client side, also, you can use the parameter just like regular functions because `FunctionN` types are implicitly converted to `JsonRpcFunctionN` types. If you want to explicitly create `JsonRpcFunction`, you can do so by using its factory method like `JsonRpcFunction((message: String) => println(message))` or `JsonRpcFunction(uuidObserver)`.

## But how is it working? I thought it's JSON-RPC library!

To illustrate how it's wroking, let's take the following API as an example.

```scala
trait FooApi {
  def foo(bar: JsonRpcFunction1[String, Future[String]]): Future[String]
}
```

When you invoke an `foo` function from client, passing your function as `bar`, it will need to send a JSON-RPC request. Because we cannot send functions over JSON, the libarary will send a generated universally unique method name for the `bar` function.

```json
{
  "jsonrpc": "2.0",
  "id": "<UUID>",
  "method": "FooApi.foo",
  "params": ["<generated.universally.unique.method.name.for.the.bar.function>"]
}
```

When server receives it, it will create a JSON-RPC client for the given method, then pass it to the API implementation. So when server invokes `bar`, it will send another JSON-RPC request to the client.

```json
{
  "jsonrpc": "2.0",
  "id": "<UUID>",
  "method": "<generated.universally.unique.method.name.for.the.bar.function>",
  "params": ["<whatever passed to the bar function>"]
}
```

Knowing this rule, you can use these APIs with JSON-RPC client & server on different technology stack too.

## End to end example over WebSocket

We also have an end to end example using WebSocket. Please refer to [its example page](../e2eWebSocket).

## Summary

- You can pass functions as parameter by using `JsonRpcFunctionN` type.
- To use function as parameter, you need to use `JsonRpcServerAndClient`.
- Invoking `JsonRpcFunction` sends just another JSON-RPC request.
    - Your function need to return either `Unit` or `Future`.
- If you pass the same function reference on client side, it will be the same function reference on server side too.
- :exclamation: **You need to manually dispose the function when you no longer need it.**
