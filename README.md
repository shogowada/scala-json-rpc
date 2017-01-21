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

## Dependency

|Platform|SBT|Scala Version|Scala JS Version|
|---|---|---|---|
|JVM|```"io.github.shogowada" %% "scala-json-rpc" % "0.4.2"```|2.12||
|JS|```"io.github.shogowada" %%% "scala-json-rpc" % "0.4.2"```|2.12|0.6|

scala-json-rpc has **no external dependency**, so it should fit into any of your Scala JVM & JS applications.

## Tutorials

- [Basic](/tutorials/Basic.md)
- [Custom JSON-RPC method name](/tutorials/CustomJsonRpcMethodName.md)
- [Passing function as parameter](/tutorials/PassingFunctionAsParameter.md) :tada:

## Examples

- [Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP](/examples/e2e)
- [Bidirectional JSON-RPC between Scals JS and Scala JVM over WebSocket](/examples/e2eWebSocket)

## Limitations

It should already serve you well as a RPC library, but it still does not fully support JSON-RPC spec yet. Here are list of known JSON-RPC features that's not supported yet.

- Send/receive named parameter
    - Define custom parameter name
- Send/receive custom JSON-RPC error
- Define custom JSON-RPC request ID
