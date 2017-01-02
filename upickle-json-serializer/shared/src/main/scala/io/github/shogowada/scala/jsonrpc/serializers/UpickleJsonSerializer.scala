package io.github.shogowada.scala.jsonrpc.serializers

import upickle.Js

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonRpcPickler extends upickle.AttributeTagged {
  override implicit def OptionW[T: Writer]: Writer[Option[T]] = {
    Writer {
      case None => Js.Null
      case Some(value) => implicitly[Writer[T]].write(value)
    }
  }

  override implicit def OptionR[T: Reader]: Reader[Option[T]] = {
    Reader {
      case Js.Null => None
      case value: Js.Value => Some(implicitly[Reader[T]].read(value))
    }
  }

  implicit def IdW: Writer[Either[String, BigDecimal]] = {
    Writer[Either[String, BigDecimal]] {
      case Left(value) => writeJs(value)
      case Right(value) => writeJs(value)
    }
  }

  implicit def IdR: Reader[Either[String, BigDecimal]] = {
    Reader[Either[String, BigDecimal]] {
      case value: Js.Str => Left(readJs[String](value))
      case value: Js.Num => Right(readJs[BigDecimal](value))
    }
  }
}

class UpickleJsonSerializer extends JsonSerializer {
  override def serialize[T](value: T): Option[String] = macro UpickleJsonSerializerMacro.serialize[T]

  override def deserialize[T](json: String): Option[T] = macro UpickleJsonSerializerMacro.deserialize[T]
}

object UpickleJsonSerializer {
  def apply() = new UpickleJsonSerializer
}


object UpickleJsonSerializerMacro {
  def serialize[T](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Option[String]] = {
    import c.universe._

    c.Expr[Option[String]](
      q"""
          import scala.util.Try
          import io.github.shogowada.scala.jsonrpc.serializers.JsonRpcPickler._
          Try(write($value)).toOption
          """
    )
  }

  def deserialize[T: c.WeakTypeTag](c: blackbox.Context)(json: c.Expr[String]): c.Expr[Option[T]] = {
    import c.universe._

    val deserializeType = weakTypeOf[T]

    c.Expr[Option[T]](
      q"""
          import scala.util.Try
          import io.github.shogowada.scala.jsonrpc.serializers.JsonRpcPickler._
          Try(read[$deserializeType]($json)).toOption
          """
    )
  }
}
