# Define custom JSON serialization logic

To define custom JSON serialization logic, you need to implement your own ```JsonSerializer```.

```scala
class MyJsonSerializer extends JsonSerializer {
  override def serialize[T](value: T): Option[String] = {
    // ... Serialize the value into JSON
  }

  override def deserialize[T](json: String): Option[T] = {
    // ... Deserialize the JSON into value
  }
}
```

Your serializer needs to meet the following requirements:

- Serialize/deserialize ```Left[String, BigDecimal]``` as JSON string.
- Serialize/deserialize ```Right[String, BigDecimal]``` as JSON number.
- Serialize/deserialize ```Some[JsonRpcError]``` as JSON object representing ```JsonRpcError```.
- Serialize/deserialize ```None[JsonRpcError]``` as JSON null.
