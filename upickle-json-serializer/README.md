# upickle-json-serializer

|SBT|Scala Version|Scala JS Version|
|---|---|---|
|```"io.github.shogowada" %%% "scala-json-rpc-upickle-json-serializer" % "0.4.2"```|2.12|0.6|

You can use upickle-json-serializer to use upickle as your ```JsonSerializer```.

```scala
val jsonSerializer = UpickleJsonSerializer()

val server = JsonRpcServer(jsonSerializer)

val jsonSender = // ...
val client = JsonRpcClient(jsonSerializer, jsonSender)
```
