# Custom JSON-RPC method name

By default, method name is full name of the method. For example, if you have an API like below:

```scala
package io.github.shogowada

trait API {
  def foo: Unit
}
```

Then JSON-RPC method name for the API method ```foo``` will be ```io.github.shogowada.API.foo```. If you are using this library only to achieve RMI between your Scala components, this is convenient to guarantee uniqueness of the method names.

But sometimes you want to define custom method names so that it integrates well with other components written with different technologies.

To define custome method name, you can annotate API method with ```@api.JsonRpcMethod``` annotation. For example, if you define your API like below:

```scala
package io.github.shogowada

import io.github.shogowada.scala.jsonrpc.api

trait API {
  @api.JsonRpcMethod(name = "foo")
  def foo: Unit
}
```

Then JSON-RPC method name for the API method ```foo``` will be ```foo``` instead of its full name.
