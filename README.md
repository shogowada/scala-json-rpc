# scala-json-rpc

master: [![Master Build Status](https://travis-ci.org/shogowada/scala-json-rpc.svg?branch=master)](https://travis-ci.org/shogowada/scala-json-rpc)

Let your servers and clients communicate over function calls!

scala-json-rpc is a Remote Procedure Call (RPC) library honorring [JSON-RPC 2.0 spec](http://www.jsonrpc.org).

JSON-RPC defines a specification of RPC in JSON format. This means that you can achieve RPC between your components as long as they are capable of

- Serializing and deserializing JSON
- Passing strings around

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
trait LoggerAPI {
  def log(message: String): Unit
}

case class Foo(id: String)

trait FooRepositoryAPI {
  def add(foo: Foo): Future[Unit]
  def remove(foo: Foo): Future[Unit]
  def getAll(): Future[Set[Foo]]
}
```

### Server

```scala
class LoggerAPIImpl extends LoggerAPI {
  override def log(message: String): Unit = println(message)
}

class FooRepositoryAPIImpl extends FooRepositoryAPI {
  var foos: Set[Foo] = Set()

  override def add(foo: Foo): Future[Unit] = this.synchronized {
    foos = foos + foo
    Future() // Acknowledge
  }

  override def remove(foo: Foo): Future[Unit] = this.synchronized {
    foos = foos - foo
    Future() // Acknowledge
  }

  override def getAll(): Future[Set[Foo]] = Future {
    foos
  }
}

val jsonSerializer = // ...
val server = JSONRPCServer(jsonSerializer)
server.bindAPI[LoggerAPI](new LoggerAPIImpl)
server.bindAPI[FooRepositoryAPI](new FooRepositoryAPIImpl)

def onRequestJSONReceived(requestJSON: String): Unit = {
  server.receive(requestJSON).onComplete {
    case Success(Some(responseJSON: String)) => sendResponseJSONToClient(responseJSON)
    case _ =>
  }
}
```

### Client

```scala
val jsonSerializer = // ...
val jsonSender = // ...
val client = JSONRPCClient(jsonSerializer, jsonSender)

val loggerAPI = client.createAPI[LoggerAPI]
val fooRepositoryAPI = client.createAPI[FooRepositoryAPI]

loggerAPI.log("Hello, World!")

fooRepositoryAPI.add(Foo("A"))
fooRepositoryAPI.add(Foo("B"))

fooRepositoryAPI.remove(Foo("A"))

fooRepositoryAPI.getAll().onComplete {
  case Success(foos: Set[Foo]) => println(s"Received all the foos: $foos")
  case _ =>
}

def onResponseJSONReceived(responseJSON: String): Unit = {
  client.receive(responseJSON)
}
```

## Dependency

|Platform|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|JVM|```"io.github.shogowada" %% "scala-json-rpc" % "1.0.0"```|2.12||
|JS|```"io.github.shogowada" %%% "scala-json-rpc" % "1.0.0"```|2.12|0.6.28+|

scala-json-rpc has **no external dependency**, so it should fit into any of your Scala JVM & JS applications.

## Tutorials

- [Basic](/tutorials/basic.md)
- [Custom JSON-RPC method name](/tutorials/custom-json-rpc-method-name.md)
- [Passing function as parameter or return value or both](/tutorials/passing-function-as-parameter-or-return-value-or-both.md) :tada:

## Examples

- [Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP](/examples/e2e)
- [Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket](/examples/e2e-web-socket)

## TODOs

It should already serve you well as a RPC library, but it still does not fully support JSON-RPC spec yet. Here are list of known JSON-RPC features that's not supported yet.

- Send/receive named parameter
    - Define custom parameter name
- Send/receive custom JSON-RPC error
- Define custom JSON-RPC request ID
