# JSON-RPC notification

To create a notification method, create an API method that returns ```Unit```.

```scala
trait LoggerApi {
  def log(message: String): Unit
}

class LoggerApiImpl extends LoggerApi {
  override def log(message: String): Unit = {
    println(message)
  }
}
```

When notification method is invoked, JSON-RPC server does not return response.

```scala
val loggerApi = new LoggerApiImpl
val server = JsonRpcServer(/* ... */)
server.bindApi[LoggerApi](loggerApi)

val json: String = // ... JSON-RPC notification
server.receive(json).onComplete {
  case Success(None) => // Server won't respond because it's notification.
  case _ =>
}
```
