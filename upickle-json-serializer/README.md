# upickle-json-serializer

|Platform|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|JVM|```"io.github.shogowada" %% "scala-json-rpc-upickle-json-serializer" % "0.6.0"```|2.12||
|JS|```"io.github.shogowada" %%% "scala-json-rpc-upickle-json-serializer" % "0.6.0"```|2.12|0.6.14+|

You can use upickle-json-serializer to use upickle as your ```JSONSerializer```.

```scala
val jsonSerializer = UpickleJSONSerializer()

val server = JSONRPCServer(jsonSerializer)

val jsonSender = // ...
val client = JSONRPCClient(jsonSerializer, jsonSender)
```
