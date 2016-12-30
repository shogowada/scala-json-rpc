# JSON-RPC notification

To create a notification method, create an API method that returns ```Unit```.

```scala
trait Api {
  def notify(message: String): Unit
}

class ApiImpl extends Api {
  override def notify(message: String): Unit = {
    println(message)
  }
}
```

When notification method is invoked, JSON-RPC server does not return response.

```scala
val api = new ApiImpl
val serverBuilder = JsonRpcServerBuilder(/* ... */)
serverBuilder.bindApi[Api](api)
val server = serverBuilder.build
val json: String = // ... JSON-RPC notification
server.receive(json).onComplete {
  case Success(None) => // Server won't respond because it's notification.
  case _ =>
}
```
