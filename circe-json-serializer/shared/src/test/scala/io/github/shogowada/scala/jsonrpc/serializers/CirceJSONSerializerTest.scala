package io.github.shogowada.scala.jsonrpc.serializers

import io.circe.generic.extras.Configuration
import org.scalatest.{OneInstancePerTest, Matchers, FreeSpec}

class CirceJSONSerializerTest extends FreeSpec
    with OneInstancePerTest
    with Matchers
{
  override def newInstance = new CirceJSONSerializerTest

  val jsonSerializer = CirceJSONSerializer()

  "given I have automatic derivation, and snake-case keys configured" - {
    import io.circe.generic.extras.auto._
    implicit val conf = Configuration.default.withSnakeCaseKeys

    case class Test(helloWorld: String)
    val expectedDeserialized = Test("hello world")
    val expectedSerialized = """{"hello_world":"hello world"}"""

    s"when I serialize $expectedDeserialized" - {
      val serialized = jsonSerializer.serialize(expectedDeserialized)

      "then it should serialize as expected" in {
        serialized should equal(Some(expectedSerialized))
      }
    }

    s"when I deserialize $expectedSerialized" - {
      val deserialized = jsonSerializer.deserialize[Test](expectedSerialized)

      "then it should deserialize as expected" in {
        deserialized should equal(Some(expectedDeserialized))
      }
    }
  }
}
