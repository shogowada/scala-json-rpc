# Basic

[JSON-RPC](http://www.jsonrpc.org) defines a specification of Remote Procedure Call (RPC) in JSON format. This means that you can achieve RPC between your components as long as they are capable of:

- serializing and deserializing JSON and
- passing strings around.

```
+--------+                          +--------+
|        | ---[request as JSON]---> |        |
| Client |                          | Server |
|        | <--[response as JSON]--- |        |
+--------+                          +--------+
```

scala-json-rpc honors the JSON-RPC 2.0 spec, so using scala-json-rpc, you can also achieve RPC as long as you meet the above 2 requirements.

## API

In scala-json-rpc, your shared contract between server and client is called API. API is a trait defining your method interfaces.

For example, if you want to have a repository of `Foo` and make it available to client, your API might look like this:

```scala
case class Foo(bar: String)

trait FooRepositoryApi {
  def add(foo: Foo): Unit
  def remove(foo: Foo): Unit
  def getAll(): Future[Set[Foo]]
}
```

If your method returns `Future[_]`, it will be JSON-RPC request, meaning you will get response from server.

It needs to return `Future[_]` because, by definition, RPC happens remotely, which means:

- You will always get response in the future.
- Your RPC might fail, in which case the `Future[_]` can fail.

If your method returns `Unit`, it will be JSON-RPC notification, meaning you don't get any response including errors. If you don't need anything returned but still want to know if the RPC succeeded, you can let it return `Future[Unit]`.

```scala
trait FooRepositoryApi {
  def add(foo: Foo): Future[Unit]
  def remove(foo: Foo): Future[Unit]
  def getAll(): Future[Set[Foo]]
}
```

## JSON-RPC client

## JSON-RPC server

On server, your implementation of the `FooRepositoryApi` might look like this:

```scala
class FooRepositoryApiImpl extends FooRepositoryApi {
  var foos: Set[Foo] = Set()

  def add(foo: Foo): Future[Unit] = this.synchronized {
    foos = foos + foo
    Future()
  }

  def remove(foo: Foo): Future[Unit] = this.synchronized {
    foos = foos - foo
    Future()
  }

  def getAll(): Future[Set[Foo]] = {
    Future(foos)
  }
}
```

Once you have an API implementation, you can bind it to the `JsonRpcServer`. `JsonRpcServer` takes care of the gluing that needs to happen between your API implementation and JSON-RPC spec.

Because the RPC is JSON base, for `JsonRpcServer` to work, you need to define how you want to serialize and deserialize JSON into your Scala models.

To do so, you can implement `JsonSerializer` class.

```scala
class MyJsonSerializer extends JsonSerializer {
  def serialize[T](value: T): Option[String] = ???
  def deserialize[T](json: String): Option[T] = ???
}
```

If you just want it to work, you can also use `UpickleJsonSerializer`. See [its README page](../upickle-json-serializer) for details.

Once you have

## JSON-RPC server and client
