package io.github.shogowada.scala.jsonrpc

import scala.concurrent.Future
import scala.language.implicitConversions

trait JsonRpcFunction[Function] {
  val original: Function

  def dispose(): Future[Unit] = {
    throw new UnsupportedOperationException("dispose method must be invoked by server")
  }
}

trait JsonRpcFunction0[R]
    extends Function0[R]
        with JsonRpcFunction[Function0[R]]

trait JsonRpcFunction1[T1, R]
    extends Function1[T1, R]
        with JsonRpcFunction[Function1[T1, R]]

trait JsonRpcFunction2[T1, T2, R]
    extends Function2[T1, T2, R]
        with JsonRpcFunction[Function2[T1, T2, R]]

trait JsonRpcFunction3[T1, T2, T3, R]
    extends Function3[T1, T2, T3, R]
        with JsonRpcFunction[Function3[T1, T2, T3, R]]

trait JsonRpcFunction4[T1, T2, T3, T4, R]
    extends Function4[T1, T2, T3, T4, R]
        with JsonRpcFunction[Function4[T1, T2, T3, T4, R]]

trait JsonRpcFunction5[T1, T2, T3, T4, T5, R]
    extends Function5[T1, T2, T3, T4, T5, R]
        with JsonRpcFunction[Function5[T1, T2, T3, T4, T5, R]]

trait JsonRpcFunction6[T1, T2, T3, T4, T5, T6, R]
    extends Function6[T1, T2, T3, T4, T5, T6, R]
        with JsonRpcFunction[Function6[T1, T2, T3, T4, T5, T6, R]]

trait JsonRpcFunction7[T1, T2, T3, T4, T5, T6, T7, R]
    extends Function7[T1, T2, T3, T4, T5, T6, T7, R]
        with JsonRpcFunction[Function7[T1, T2, T3, T4, T5, T6, T7, R]]

trait JsonRpcFunction8[T1, T2, T3, T4, T5, T6, T7, T8, R]
    extends Function8[T1, T2, T3, T4, T5, T6, T7, T8, R]
        with JsonRpcFunction[Function8[T1, T2, T3, T4, T5, T6, T7, T8, R]]

trait JsonRpcFunction9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]
    extends Function9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]
        with JsonRpcFunction[Function9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]]

trait JsonRpcFunction10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]
    extends Function10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]
        with JsonRpcFunction[Function10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]]

trait JsonRpcFunction11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]
    extends Function11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]
        with JsonRpcFunction[Function11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]]

trait JsonRpcFunction12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]
    extends Function12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]
        with JsonRpcFunction[Function12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]]

trait JsonRpcFunction13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]
    extends Function13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]
        with JsonRpcFunction[Function13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]]

trait JsonRpcFunction14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]
    extends Function14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]
        with JsonRpcFunction[Function14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]]

trait JsonRpcFunction15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]
    extends Function15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]
        with JsonRpcFunction[Function15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]]

trait JsonRpcFunction16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]
    extends Function16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]
        with JsonRpcFunction[Function16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]]

trait JsonRpcFunction17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]
    extends Function17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]
        with JsonRpcFunction[Function17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]]

trait JsonRpcFunction18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]
    extends Function18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]
        with JsonRpcFunction[Function18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]]

trait JsonRpcFunction19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]
    extends Function19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]
        with JsonRpcFunction[Function19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]]

trait JsonRpcFunction20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]
    extends Function20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]
        with JsonRpcFunction[Function20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]]

trait JsonRpcFunction21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R]
    extends Function21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R]
        with JsonRpcFunction[Function21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R]]

trait JsonRpcFunction22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R]
    extends Function22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R]
        with JsonRpcFunction[Function22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R]]

// JsonRpcServerFunction equivalent will be constructed by macro

object JsonRpcFunction {
  implicit def apply[R](
      function: Function0[R]
  ): JsonRpcFunction0[R] = {
    new JsonRpcFunction0[R] {
      override val original = function

      override def apply(): R =
        function()
    }
  }

  implicit def apply[T1, R](
      function: Function1[T1, R]
  ): JsonRpcFunction1[T1, R] = {
    new JsonRpcFunction1[T1, R] {
      override val original = function

      override def apply(v1: T1): R =
        function(v1)
    }
  }

  implicit def apply[T1, T2, R](
      function: Function2[T1, T2, R]
  ): JsonRpcFunction2[T1, T2, R] = {
    new JsonRpcFunction2[T1, T2, R] {
      override val original = function

      override def apply(v1: T1, v2: T2): R =
        function(v1, v2)
    }
  }

  implicit def apply[T1, T2, T3, R](
      function: Function3[T1, T2, T3, R]
  ): JsonRpcFunction3[T1, T2, T3, R] = {
    new JsonRpcFunction3[T1, T2, T3, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3): R =
        function(v1, v2, v3)
    }
  }

  implicit def apply[T1, T2, T3, T4, R](
      function: Function4[T1, T2, T3, T4, R]
  ): JsonRpcFunction4[T1, T2, T3, T4, R] = {
    new JsonRpcFunction4[T1, T2, T3, T4, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4): R =
        function(v1, v2, v3, v4)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, R](
      function: Function5[T1, T2, T3, T4, T5, R]
  ): JsonRpcFunction5[T1, T2, T3, T4, T5, R] = {
    new JsonRpcFunction5[T1, T2, T3, T4, T5, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5): R =
        function(v1, v2, v3, v4, v5)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, R](
      function: Function6[T1, T2, T3, T4, T5, T6, R]
  ): JsonRpcFunction6[T1, T2, T3, T4, T5, T6, R] = {
    new JsonRpcFunction6[T1, T2, T3, T4, T5, T6, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6): R =
        function(v1, v2, v3, v4, v5, v6)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, R](
      function: Function7[T1, T2, T3, T4, T5, T6, T7, R]
  ): JsonRpcFunction7[T1, T2, T3, T4, T5, T6, T7, R] = {
    new JsonRpcFunction7[T1, T2, T3, T4, T5, T6, T7, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7): R =
        function(v1, v2, v3, v4, v5, v6, v7)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, R](
      function: Function8[T1, T2, T3, T4, T5, T6, T7, T8, R]
  ): JsonRpcFunction8[T1, T2, T3, T4, T5, T6, T7, T8, R] = {
    new JsonRpcFunction8[T1, T2, T3, T4, T5, T6, T7, T8, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](
      function: Function9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]
  ): JsonRpcFunction9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] = {
    new JsonRpcFunction9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](
      function: Function10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]
  ): JsonRpcFunction10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] = {
    new JsonRpcFunction10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](
      function: Function11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]
  ): JsonRpcFunction11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] = {
    new JsonRpcFunction11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](
      function: Function12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]
  ): JsonRpcFunction12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] = {
    new JsonRpcFunction12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](
      function: Function13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]
  ): JsonRpcFunction13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] = {
    new JsonRpcFunction13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](
      function: Function14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]
  ): JsonRpcFunction14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] = {
    new JsonRpcFunction14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](
      function: Function15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]
  ): JsonRpcFunction15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] = {
    new JsonRpcFunction15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](
      function: Function16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]
  ): JsonRpcFunction16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] = {
    new JsonRpcFunction16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](
      function: Function17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]
  ): JsonRpcFunction17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] = {
    new JsonRpcFunction17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](
      function: Function18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]
  ): JsonRpcFunction18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] = {
    new JsonRpcFunction18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](
      function: Function19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]
  ): JsonRpcFunction19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] = {
    new JsonRpcFunction19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](
      function: Function20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]
  ): JsonRpcFunction20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] = {
    new JsonRpcFunction20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19, v20: T20): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R](
      function: Function21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R]
  ): JsonRpcFunction21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R] = {
    new JsonRpcFunction21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19, v20: T20, v21: T21): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R](
      function: Function22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R]
  ): JsonRpcFunction22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R] = {
    new JsonRpcFunction22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R] {
      override val original = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19, v20: T20, v21: T21, v22: T22): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22)
    }
  }
}
