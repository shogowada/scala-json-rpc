# upickle-json-serializer

You can use upickle-json-serializer to use upickle as your ```JsonSerializer```.

```scala
val jsonSerializer = UpickleJsonSerializer()

val serverBuilder = JsonRpcServerBuilder(jsonSerializer)
serverBuilder.bindApi[FooApi](api)

val server = serverBuilder.build

val clientBuilder = JsonRpcClientBuilder(
  jsonSerializer,
  (json: String) => server.receive(json)
)

val client = clientBuilder.build
```
