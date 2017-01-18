# Passing function as parameter

You can pass functions as parameter by using `JsonRpcFunction` with `JsonRpcServerAndClient`. This is useful if you want callbacks from server.

## Create `JsonRpcServerAndClient`

To create `JsonRpcServerAndClient`, you need to create a server and a client first like you normally would.

```scala
val jsonSerializer = new MyJsonSerializer()
val jsonSender: (String) => Future[Option[String]] = // ... Implement JSON sender
val jsonRpcServer: JsonRpcServer[MyJsonSerializer] = JsonRpcServer(jsonSerializer)
val jsonRpcClient: JsonRpcClient[MyJsonSerializer] = JsonRpcClient(jsonSerializer, jsonSender)
```

Then, you can create `JsonRpcServerAndClient` using the server and the client.

```scala
val jsonRpcServerAndClient: JsonRpcServerAndClient[MyJsonSerializer] =
    JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
```

Because you need to be both server and client, your communication protocol must be bidirectional.

## Create API that takes `JsonRpcFunction` as parameter

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
    callback.dispose() // Make sure to dispose the function when you no longer need it
  }
}

class UuidSubjectApiImpl extends UuidSubjectApi {
  var observers: Set[JsonRpcFunction1[String, Future[Unit]]] = Set()
  
  /* ... Set timer so that we will invoke the following method periodically */
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
    observer.dispose() // Once unregistered, we no longer need it.
  }
}

val serverAndClient = JsonRpcServerAndClient(/* ... */)
serverAndClient.bindApi[EchoApi](new EchoApiImpl)
serverAndClient.bindApi[UuidSubjectApi](new UuidSubjectApiImpl)
```

You can use the `JsonRpcFunction` just like regular function except **you need to explicitly dispose the function when you no longer need it**. This is so that both server and client can dispose relative mappings when we no longer need them.

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

## End to end example over WebSocket

We also have an end to end example using WebSocket. Please refer to [its example page](../e2eWebSocket).
