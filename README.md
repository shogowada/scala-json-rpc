# scala-json-rpc

Make communication between your server and client as easy as making function calls!

scala-json-rpc is a Remote Procedure Call (RPC) library honorring [JSON-RPC 2.0 spec](http://www.jsonrpc.org).

JSON-RPC defines a specification of RPC in JSON format. This means that you can achieve RPC between your components as long as they are capable of:

- serializing and deserializing JSON.
- passing strings around.

```
+--------+                          +--------+
|        | ---[request as JSON]---> |        |
| Client |                          | Server |
|        | <--[response as JSON]--- |        |
+--------+                          +--------+
```

## Quick look

### Shared between server and client

Using scala-json-rpc, your server and client can communicate over statically typed interfaces like below:

```scala
trait LoggerApi {
  def log(message: String): Unit
}

case class Foo(id: String)

trait FooRepositoryApi {
  def add(foo: Foo): Future[Unit]
  def remove(foo: Foo): Future[Unit]
  def getAll(): Future[Set[Foo]]
}
```

### Server

```scala
class LoggerApiImpl extends LoggerApi {
  override def log(message: String): Unit = println(message)
}

class FooRepositoryApiImpl extends FooRepositoryApi {
  var foos: Set[Foo] = Set()

  override def add(foo: Foo): Future[Unit] = this.synchronized {
    foos = foos + foo
    Future() // Acknowledge
  }

  override def remove(foo: Foo): Future[Unit] = this.synchronized {
    foos = foos - foo
    Future() // Acknowledge
  }

  override def getAll(): Future[Set[Foo]] = {
    Future(foos)
  }
}

val jsonSerializer = // ...
val server = jsonRpcServer(jsonSerializer)
server.bindApi[LoggerApi](new LoggerApiImpl)
server.bindApi[FooRepositoryApi](new FooRepositoryApiImpl)

def onRequestJsonReceived(requestJson: String): Unit = {
  server.receive(requestJson).onComplete {
    case Success(Some(responseJson: String)) => sendResponseJsonToClient(responseJson)
    case _ =>
  }
}
```

### Client

```scala
val jsonSerializer = // ...
val jsonSender = // ...
val client = JsonRpcClient(jsonSerializer, jsonSender)

val loggerApi = client.createApi[LoggerApi]
val fooRepositoryApi = client.createApi[FooRepositoryApi]

loggerApi.log("Hello, World!")
    
fooRepositoryApi.add(Foo("A"))
fooRepositoryApi.add(Foo("B"))

fooRepositoryApi.remove(Foo("A"))

fooRepositoryApi.getAll().onComplete {
  case Success(foos: Set[Foo]) => println(s"Received all the foos: $foos")
  case _ =>
}

def onResponseJsonReceived(responseJson: String) {
  client.receive(responseJson)
}
```

## Dependency

|Platform|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|JVM|```"io.github.shogowada" %% "scala-json-rpc" % "0.4.1"```|2.12||
|JS|```"io.github.shogowada" %%% "scala-json-rpc" % "0.4.1"```|2.12|0.6|

scala-json-rpc has **no external dependency**, so it should fit into any of your Scala JVM & JS applications.

## Tutorials

- [Basic](/tutorials/Basic.md)
- [Custom JSON-RPC method name](/tutorials/CustomJsonRpcMethodName.md)
- [Passing function as parameter](/tutorials/PassingFunctionAsParameter.md) :tada:

## Examples

- [Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP](/examples/e2e)
- [Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket](/examples/e2eWebSocket)

## TODOs

It should already serve you well as a RPC library, but it still does not fully support JSON-RPC spec yet. Here are list of known JSON-RPC features that's not supported yet.

- Send/receive named parameter
    - Define custom parameter name
- Send/receive custom JSON-RPC error
- Define custom JSON-RPC request ID
