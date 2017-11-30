# circe-json-serializer

|Platform|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|JVM|```"io.github.shogowada" %% "scala-json-rpc-circe-json-serializer" % "0.9.2"```|2.12||
|JS|```"io.github.shogowada" %%% "scala-json-rpc-circe-json-serializer" % "0.9.2"```|2.12|0.6.17+|

You can use circe-json-serializer to use [circe](https://github.com/circe/circe) as your ```JSONSerializer```.

```scala
// optional for custom key mappings https://circe.github.io/circe/codec.html#custom-key-mappings-via-annotations
import io.circe.generic.extras.Configuration
implicit val conf = Configuration.default.withSnakeCaseKeys

// optional for automatic derivation https://circe.github.io/circe/codec.html#fully-automatic-derivation
import io.circe.generic.extras.auto._

val jsonSerializer = CirceJSONSerializer()

val server = JSONRPCServer(jsonSerializer)

val jsonSender = // ...
val client = JSONRPCClient(jsonSerializer, jsonSender)
```
