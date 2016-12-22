package io.github.shogowada.scala.jsonrpc.serializers

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class UpickleJsonSerializer {
  def serialize[T](value: T): Option[String] = macro UpickleJsonSerializerMacro.serializeImpl[T]

  def deserialize[T](json: String): Option[T] = macro UpickleJsonSerializerMacro.deserializeImpl[T]
}

object UpickleJsonSerializer {
  def apply() = new UpickleJsonSerializer
}


object UpickleJsonSerializerMacro {
  def serializeImpl[T](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Option[String]] = {
    import c.universe._

    c.Expr[Option[String]](
      q"""
          import upickle.default._
          Option(write($value))
          """
    )
  }

  def deserializeImpl[T: c.WeakTypeTag](c: blackbox.Context)(json: c.Expr[String]): c.Expr[Option[T]] = {
    import c.universe._

    val deserializeType = weakTypeOf[T]

    c.Expr[Option[T]](
      q"""
          import upickle.default._
          Option(read[$deserializeType]($json))
          """
    )
  }
}
