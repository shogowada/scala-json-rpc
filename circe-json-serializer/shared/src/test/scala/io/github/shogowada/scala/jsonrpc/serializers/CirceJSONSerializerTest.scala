package io.github.shogowada.scala.jsonrpc.serializers

import org.scalatest.{FreeSpec, Matchers, OneInstancePerTest}
import io.circe.generic.auto._

class CirceJSONSerializerTest extends FreeSpec
    with OneInstancePerTest
    with Matchers {
  override def newInstance = new CirceJSONSerializerTest

  val jsonSerializer = CirceJSONSerializer()

  "given I have a plain case class" - {
    case class Test(helloWorld: String)
    val expectedDeserialized = Test("hello world")
    val expectedSerialized = """{"helloWorld":"hello world"}"""

    s"when I serialize $expectedDeserialized" - {
      val serialized = jsonSerializer.serialize(expectedDeserialized)

      "then it should serialize into JSON" in {
        serialized should equal(Some(expectedSerialized))
      }
    }

    s"when I deserialize $expectedSerialized" - {
      val deserialized = jsonSerializer.deserialize[Test](expectedSerialized)

      "then it should deserialize into the case class" in {
        deserialized should equal(Some(expectedDeserialized))
      }
    }
  }
}
