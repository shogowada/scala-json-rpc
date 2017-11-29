# roshttp-json-rpc-client

|Platform|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|JVM|```"io.github.shogowada" %% "scala-json-rpc-roshttp-json-rpc-client" % "0.9.1"```|2.12||
|JS|```"io.github.shogowada" %%% "scala-json-rpc-roshttp-json-rpc-client" % "0.9.1"```|2.12|0.6.15+|

You can use roshttp-json-rpc-client to use [RösHTTP](https://github.com/hmil/RosHTTP) as your ```JSONRPCClient``` (serializer sold separately).

```scala
val jsonSerializer = ??? // serializer sold separately

val server = JSONRPCServer(jsonSerializer)

import monix.execution.Scheduler.Implicits.global

val client = RosHTTPJSONRPCClient(
    jsonSerializer,
    "http://json.rpc/endpoint",
    "Optional" → "additional",
    "Http" → "headers"
)
```
