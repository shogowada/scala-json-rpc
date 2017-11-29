package io.github.shogowada.scala.jsonrpc.serializers

import io.circe.{Encoder, Decoder, Error, Json}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object CirceJSONCoder {
  def encode[T](value: T)(implicit encoder: Encoder[T]): Json = {
    encoder(value)
  }

  def decode[T](json: String)(implicit decoder: Decoder[T]): Either[Error, T] =
  {
    io.circe.parser.decode[T](json)
  }
}

class CirceJSONSerializer extends JSONSerializer {
  override def serialize[T](value: T): Option[String] = macro CirceJSONSerializerMacro.serialize[T]

  override def deserialize[T](json: String): Option[T] = macro CirceJSONSerializerMacro.deserialize[T]
}

object CirceJSONSerializer {
  def apply() = {
    new CirceJSONSerializer
  }
}

object CirceJSONSerializerMacro {
  def serialize[T](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Option[String]] = {
    import c.universe._

    c.Expr[Option[String]](
      q"""
          scala.util.Try(io.circe.Printer.noSpaces.pretty(io.github.shogowada.scala.jsonrpc.serializers.CirceJSONCoder.encode($value))).toOption
          """
    )
  }

  def deserialize[T: c.WeakTypeTag](c: blackbox.Context)(json: c.Expr[String]): c.Expr[Option[T]] = {
    import c.universe._

    val deserializeType = weakTypeOf[T]

    c.Expr[Option[T]](
      q"""
          io.github.shogowada.scala.jsonrpc.serializers.CirceJSONCoder.decode[$deserializeType]($json).toOption
          """
    )
  }
}
