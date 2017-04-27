# Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket

When your application is both server and client, incoming JSON messages can be either request or response, so you need to make sure requests are received by server and responses are received by client.

You can write something like below to make sure the JSON is handled appropriately.

```scala
val wasJSONRPCResponse: Boolean = jsonRPCClient.receive(json)
if (!wasJSONRPCResponse) {
  jsonRPCServer.receive(json).onComplete {
    case Success(Some(responseJSON: String)) => jsonRPCClient.send(responseJSON)
    case _ =>
  }
}
```

But it is tedious and error prone to write this everytime you implement bidirectional JSON-RPC.

Because this is such a common use case, we have an API called `JSONRPCServerAndClient` for this purpose, so we recommend using it.

```scala
val serverAndClient = JSONRPCServerAndClient(jsonRPCServer, jsonRPCClient)
serverAndClient.receiveAndSend(json)
```

`jsonRPCServerAndClient.receiveAndSend` makes sure to:

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

trait TodoRepositoryAPI {
  def add(description: String): Future[Todo]

  def remove(id: String): Future[Unit]

  def register(observer: DisposableFunction1[TodoEvent, Future[Unit]]): Future[String]

  def unregister(observerId: String): Future[Unit]
}
```

Note that we are using `DisposableFunction1` to callback the client. For more details about `DisposableFunction`, please see [its tutorial page](../../tutorials/passing-function-as-parameter.md).

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
        serverAndClient.createAPI[TodoRepositoryAPI]
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

  private def createServerAndClient(futureWebSocket: Future[WebSocket]): JSONRPCServerAndClient[UpickleJSONSerializer] = {
    val jsonSerializer = UpickleJSONSerializer()

    val server = JSONRPCServer(jsonSerializer)

    val jsonSender: JSONSender = (json: String) => {
      futureWebSocket
          .map(webSocket => Try(webSocket.send(json)))
          .flatMap(tried => tried.fold(
            throwable => Future.failed(throwable),
            _ => Future(None)
          ))
    }
    val client = JSONRPCClient(jsonSerializer, jsonSender)

    val serverAndClient = JSONRPCServerAndClient(server, client)

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
class TodoRepositoryAPIImpl extends TodoRepositoryAPI {

  var todos: Seq[Todo] = Seq()
  var observersById: Map[String, DisposableFunction1[TodoEvent, Future[Unit]]] = Map()

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

  override def register(observer: DisposableFunction1[TodoEvent, Future[Unit]]): Future[String] = this.synchronized {
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

  private def notify(observerId: String, observer: DisposableFunction1[TodoEvent, Future[Unit]], todoEvent: TodoEvent): Unit = {
    observer(todoEvent)
        .failed // Probably connection is lost.
        .foreach(_ => unregister(observerId))
  }
}
```

Here is our WebSocket implementation:

```scala
object JSONRPCModule {

  lazy val todoRepositoryAPI = new TodoRepositoryAPIImpl

  lazy val jsonSerializer = UpickleJSONSerializer()

  def createJSONRPCServerAndClient(jsonSender: JSONSender): JSONRPCServerAndClient[UpickleJSONSerializer] = {
    val server = JSONRPCServer(jsonSerializer)
    val client = JSONRPCClient(jsonSerializer, jsonSender)
    val serverAndClient = JSONRPCServerAndClient(server, client)

    serverAndClient.bindAPI[TodoRepositoryAPI](todoRepositoryAPI)

    serverAndClient
  }
}

class JSONRPCWebSocket extends WebSocketAdapter {
  private var serverAndClient: JSONRPCServerAndClient[UpickleJSONSerializer] = _

  override def onWebSocketConnect(session: Session): Unit = {
    super.onWebSocketConnect(session)

    val jsonSender: JSONSender = (json: String) => {
      Try(session.getRemote.sendString(json)).fold(
        throwable => Future.failed(throwable),
        _ => Future(None)
      )
    }

    // Create an independent server and client for each WebSocket session.
    // This is to make sure we clean up all the caches (e.g. promised response, etc)
    // on each WebSocket session.
    serverAndClient = JSONRPCModule.createJSONRPCServerAndClient(jsonSender)
  }

  override def onWebSocketText(message: String): Unit = {
    serverAndClient.receiveAndSend(message)
  }
}
```
