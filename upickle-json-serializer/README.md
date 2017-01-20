# upickle-json-serializer

|SBT|Scala Version|Scala JS Version|
|---|---|---|
|```"io.github.shogowada" %%% "scala-json-rpc-upickle-json-serializer" % "0.4.2"```|2.12|0.6|

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
