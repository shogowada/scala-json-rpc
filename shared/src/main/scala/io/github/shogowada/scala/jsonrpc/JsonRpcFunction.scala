package io.github.shogowada.scala.jsonrpc

import scala.concurrent.Future
import scala.language.implicitConversions

trait JsonRpcFunction[Function] {
  val function: Function
  lazy val call: Function = function

  def dispose(): Future[Unit]
}

class JsonRpcClientFunction[Function](override val function: Function) extends JsonRpcFunction[Function] {
  override def dispose(): Future[Unit] = {
    throw new UnsupportedOperationException("dispose method is supposed to be invoked by server")
  }
}

// JsonRpcServerFunction equivalent will be constructed by macro

object JsonRpcFunction {
  def apply[Function](function: Function): JsonRpcFunction[Function] = {
    new JsonRpcClientFunction[Function](function)
  }

  implicit def function0[R](function: Function0[R]): JsonRpcFunction[Function0[R]] = {
    JsonRpcFunction(function)
  }

  implicit def function1[T, R](function: Function1[T, R]): JsonRpcFunction[Function1[T, R]] = {
    JsonRpcFunction(function)
  }
}
