package io.github.shogowada.scala.jsonrpc

import scala.concurrent.Future
import scala.language.implicitConversions

trait JsonRpcFunction[Function] {
  val original: Function

  def dispose(): Future[Unit] = {
    throw new UnsupportedOperationException("dispose method is supposed to be invoked by server")
  }
}

trait JsonRpcFunction0[R] extends Function0[R] with JsonRpcFunction[Function0[R]]

trait JsonRpcFunction1[T1, R] extends Function1[T1, R] with JsonRpcFunction[Function1[T1, R]]

// JsonRpcServerFunction equivalent will be constructed by macro

object JsonRpcFunction {
  implicit def function0[R](function: Function0[R]): JsonRpcFunction0[R] = {
    new JsonRpcFunction0[R] {
      override val original = function

      override def apply(): R = function()
    }
  }

  implicit def function1[T1, R](function: Function1[T1, R]): JsonRpcFunction1[T1, R] = {
    new JsonRpcFunction1[T1, R] {
      override val original = function

      override def apply(v1: T1): R = function(v1)
    }
  }
}
