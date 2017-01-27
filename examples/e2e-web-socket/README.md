# Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket

When your application is both server and client, incoming JSON messages can be either request or response, so you need to make sure requests are received by server and responses are received by client.

You can write something like below to make sure the JSON is handled appropriately.

```scala
val wasJsonRpcResponse: Boolean = jsonRpcClient.receive(json)
if (!wasJsonRpcResponse) {
  jsonRpcServer.receive(json).onComplete {
    case Success(Some(responseJson: String)) => jsonRpcClient.send(responseJson)
    case _ =>
  }
}
```

But it is tedious and error prone to write this everytime you implement bidirectional JSON-RPC.

Because this is such a common use case, we have an API called `JsonRpcServerAndClient` for this purpose, so we recommend using it.

```scala
val serverAndClient = JsonRpcServerAndClient(jsonRpcServer, jsonRpcClient)
serverAndClient.receiveAndSend(json)
```

`jsonRpcServerAndClient.receiveAndSend` makes sure to:

- handle the given JSON using the client first then the server second.
- send the response if present using the client's `send` method.

## Shared

We define the following API for this example:

```scala
case class Todo(id: String, description: String)

object TodoEventTypes {
  val Add = "Add"
  val Remove = "Remove"
}

case class TodoEvent(todo: Todo, eventType: String)

trait TodoRepositoryApi {
  def add(description: String): Future[Todo]

  def remove(id: String): Future[Unit]

  def register(observer: JsonRpcFunction1[TodoEvent, Future[Unit]]): Future[String]

  def unregister(observerId: String): Future[Unit]
}
```

Note that we are using `JsonRpcFunction1` to callback the client. For more details about `JsonRpcFunction`, please see [its example page](../tutorials/PassingFunctionAsParameter.md).

## Scala JS (browser)

Here is how client communicates with server:

```scala
object Main extends JSApp {
  override def main(): Unit = {
    val futureWebSocket = createFutureWebSocket()
    val serverAndClient = createServerAndClient(futureWebSocket)

    val mountNode = dom.document.getElementById("mount-node")
    ReactDOM.render(
      new TodoListView(
        serverAndClient.createApi[TodoRepositoryApi]
      )(TodoListView.Props()),
      mountNode
    )
  }

  private def createFutureWebSocket(): Future[WebSocket] = {
    val promisedWebSocket: Promise[WebSocket] = Promise()
    val webSocket = new dom.WebSocket(webSocketUrl)

    webSocket.onopen = (_: dom.Event) => {
      promisedWebSocket.success(webSocket)
    }

    webSocket.onerror = (event: dom.ErrorEvent) => {
      promisedWebSocket.failure(new IOException(event.message))
    }

    promisedWebSocket.future
  }

  private def webSocketUrl: String = {
    val location = dom.window.location
    val protocol = location.protocol match {
      case "http:" => "ws:"
      case "https:" => "wss:"
    }
    s"$protocol//${location.host}/jsonrpc"
  }

  private def createServerAndClient(futureWebSocket: Future[WebSocket]): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val jsonSerializer = UpickleJsonSerializer()

    val server = JsonRpcServer(jsonSerializer)

    val jsonSender: JsonSender = (json: String) => {
      futureWebSocket
          .map(webSocket => Try(webSocket.send(json)))
          .flatMap(tried => tried.fold(
            throwable => Future.failed(throwable),
            _ => Future(None)
          ))
    }
    val client = JsonRpcClient(jsonSerializer, jsonSender)

    val serverAndClient = JsonRpcServerAndClient(server, client)

    futureWebSocket.foreach(webSocket => {
      webSocket.onmessage = (event: dom.MessageEvent) => {
        val message = event.data.toString
        serverAndClient.receiveAndSend(message).onComplete {
          case Failure(throwable) => {
            println("Failed to send response", throwable)
          }
          case _ =>
        }
      }
    })

    serverAndClient
  }
}
```

## Server

Here is our API implementation:

```scala
class TodoRepositoryApiImpl extends TodoRepositoryApi {

  var todos: Seq[Todo] = Seq()
  var observersById: Map[String, JsonRpcFunction1[TodoEvent, Future[Unit]]] = Map()

  override def add(description: String): Future[Todo] = this.synchronized {
    val todo = Todo(id = UUID.randomUUID().toString, description)
    todos = todos :+ todo

    notify(TodoEvent(todo, TodoEventTypes.Add))

    Future(todo)
  }

  override def remove(id: String): Future[Unit] = this.synchronized {
    val index = todos.indexWhere(todo => todo.id == id)
    if (index >= 0) {
      val todo = todos(index)
      todos = todos.patch(index, Seq(), 1)
      notify(TodoEvent(todo, TodoEventTypes.Remove))
    }
    Future()
  }

  override def register(observer: JsonRpcFunction1[TodoEvent, Future[Unit]]): Future[String] = this.synchronized {
    val id = UUID.randomUUID().toString
    observersById = observersById + (id -> observer)

    todos.map(todo => TodoEvent(todo, TodoEventTypes.Add))
        .foreach(todoEvent => notify(id, observer, todoEvent))

    Future(id)
  }

  override def unregister(observerId: String): Future[Unit] = this.synchronized {
    observersById.get(observerId).foreach(observer => {
      observersById = observersById - observerId
      observer.dispose()
    })
    Future()
  }

  private def notify(todoEvent: TodoEvent): Unit = {
    observersById.foreach {
      case (id, observer) => notify(id, observer, todoEvent)
    }
  }

  private def notify(observerId: String, observer: JsonRpcFunction1[TodoEvent, Future[Unit]], todoEvent: TodoEvent): Unit = {
    observer(todoEvent)
        .failed // Probably connection is lost.
        .foreach(_ => unregister(observerId))
  }
}
```

Here is our WebSocket implementation:

```scala
object JsonRpcModule {

  lazy val todoRepositoryApi = new TodoRepositoryApiImpl

  lazy val jsonSerializer = UpickleJsonSerializer()

  def createJsonRpcServerAndClient(jsonSender: JsonSender): JsonRpcServerAndClient[UpickleJsonSerializer] = {
    val server = JsonRpcServer(jsonSerializer)
    val client = JsonRpcClient(jsonSerializer, jsonSender)
    val serverAndClient = JsonRpcServerAndClient(server, client)

    serverAndClient.bindApi[TodoRepositoryApi](todoRepositoryApi)

    serverAndClient
  }
}

class JsonRpcWebSocket extends WebSocketAdapter {
  private var serverAndClient: JsonRpcServerAndClient[UpickleJsonSerializer] = _

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val jsonSender: JsonSender = (json: String) => {
      Try(session.getRemote.sendString(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }

    // Create an independent server and client for each WebSocket session.
    // This is to make sure we clean up all the caches (e.g. promised response, etc)
    // on each WebSocket session.
    serverAndClient = JsonRpcModule.createJsonRpcServerAndClient(jsonSender)
  }

  override def onWebSocketText(message: String): Unit = {
    serverAndClient.receiveAndSend(message)
  }
}
```
