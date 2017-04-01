package io.github.shogowada.scala.jsonrpc

import scala.concurrent.Future
import scala.language.implicitConversions

trait DisposableFunction {
  val identifier: Any

  def dispose(): Future[Unit] = {
    throw new UnsupportedOperationException("dispose method must be invoked by server")
  }
}

trait DisposableFunction0[R]
    extends Function0[R]
        with DisposableFunction

trait DisposableFunction1[T1, R]
    extends Function1[T1, R]
        with DisposableFunction

trait DisposableFunction2[T1, T2, R]
    extends Function2[T1, T2, R]
        with DisposableFunction

trait DisposableFunction3[T1, T2, T3, R]
    extends Function3[T1, T2, T3, R]
        with DisposableFunction

trait DisposableFunction4[T1, T2, T3, T4, R]
    extends Function4[T1, T2, T3, T4, R]
        with DisposableFunction

trait DisposableFunction5[T1, T2, T3, T4, T5, R]
    extends Function5[T1, T2, T3, T4, T5, R]
        with DisposableFunction

trait DisposableFunction6[T1, T2, T3, T4, T5, T6, R]
    extends Function6[T1, T2, T3, T4, T5, T6, R]
        with DisposableFunction

trait DisposableFunction7[T1, T2, T3, T4, T5, T6, T7, R]
    extends Function7[T1, T2, T3, T4, T5, T6, T7, R]
        with DisposableFunction

trait DisposableFunction8[T1, T2, T3, T4, T5, T6, T7, T8, R]
    extends Function8[T1, T2, T3, T4, T5, T6, T7, T8, R]
        with DisposableFunction

trait DisposableFunction9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]
    extends Function9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]
        with DisposableFunction

trait DisposableFunction10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]
    extends Function10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]
        with DisposableFunction

trait DisposableFunction11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]
    extends Function11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]
        with DisposableFunction

trait DisposableFunction12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]
    extends Function12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]
        with DisposableFunction

trait DisposableFunction13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]
    extends Function13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]
        with DisposableFunction

trait DisposableFunction14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]
    extends Function14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]
        with DisposableFunction

trait DisposableFunction15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]
    extends Function15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]
        with DisposableFunction

trait DisposableFunction16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]
    extends Function16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]
        with DisposableFunction

trait DisposableFunction17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]
    extends Function17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]
        with DisposableFunction

trait DisposableFunction18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]
    extends Function18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]
        with DisposableFunction

trait DisposableFunction19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]
    extends Function19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]
        with DisposableFunction

trait DisposableFunction20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]
    extends Function20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]
        with DisposableFunction

trait DisposableFunction21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R]
    extends Function21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R]
        with DisposableFunction

trait DisposableFunction22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R]
    extends Function22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R]
        with DisposableFunction

// JsonRpcServerFunction equivalent will be constructed by macro

object DisposableFunction {
  implicit def apply[R](
      function: Function0[R]
  ): DisposableFunction0[R] = {
    new DisposableFunction0[R] {
      override val identifier = function

      override def apply(): R =
        function()
    }
  }

  implicit def apply[T1, R](
      function: Function1[T1, R]
  ): DisposableFunction1[T1, R] = {
    new DisposableFunction1[T1, R] {
      override val identifier = function

      override def apply(v1: T1): R =
        function(v1)
    }
  }

  implicit def apply[T1, T2, R](
      function: Function2[T1, T2, R]
  ): DisposableFunction2[T1, T2, R] = {
    new DisposableFunction2[T1, T2, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2): R =
        function(v1, v2)
    }
  }

  implicit def apply[T1, T2, T3, R](
      function: Function3[T1, T2, T3, R]
  ): DisposableFunction3[T1, T2, T3, R] = {
    new DisposableFunction3[T1, T2, T3, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3): R =
        function(v1, v2, v3)
    }
  }

  implicit def apply[T1, T2, T3, T4, R](
      function: Function4[T1, T2, T3, T4, R]
  ): DisposableFunction4[T1, T2, T3, T4, R] = {
    new DisposableFunction4[T1, T2, T3, T4, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4): R =
        function(v1, v2, v3, v4)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, R](
      function: Function5[T1, T2, T3, T4, T5, R]
  ): DisposableFunction5[T1, T2, T3, T4, T5, R] = {
    new DisposableFunction5[T1, T2, T3, T4, T5, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5): R =
        function(v1, v2, v3, v4, v5)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, R](
      function: Function6[T1, T2, T3, T4, T5, T6, R]
  ): DisposableFunction6[T1, T2, T3, T4, T5, T6, R] = {
    new DisposableFunction6[T1, T2, T3, T4, T5, T6, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6): R =
        function(v1, v2, v3, v4, v5, v6)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, R](
      function: Function7[T1, T2, T3, T4, T5, T6, T7, R]
  ): DisposableFunction7[T1, T2, T3, T4, T5, T6, T7, R] = {
    new DisposableFunction7[T1, T2, T3, T4, T5, T6, T7, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7): R =
        function(v1, v2, v3, v4, v5, v6, v7)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, R](
      function: Function8[T1, T2, T3, T4, T5, T6, T7, T8, R]
  ): DisposableFunction8[T1, T2, T3, T4, T5, T6, T7, T8, R] = {
    new DisposableFunction8[T1, T2, T3, T4, T5, T6, T7, T8, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, R](
      function: Function9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R]
  ): DisposableFunction9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] = {
    new DisposableFunction9[T1, T2, T3, T4, T5, T6, T7, T8, T9, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R](
      function: Function10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R]
  ): DisposableFunction10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] = {
    new DisposableFunction10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R](
      function: Function11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R]
  ): DisposableFunction11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] = {
    new DisposableFunction11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R](
      function: Function12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R]
  ): DisposableFunction12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] = {
    new DisposableFunction12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R](
      function: Function13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R]
  ): DisposableFunction13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] = {
    new DisposableFunction13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R](
      function: Function14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R]
  ): DisposableFunction14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] = {
    new DisposableFunction14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R](
      function: Function15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R]
  ): DisposableFunction15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] = {
    new DisposableFunction15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R](
      function: Function16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R]
  ): DisposableFunction16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] = {
    new DisposableFunction16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R](
      function: Function17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R]
  ): DisposableFunction17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] = {
    new DisposableFunction17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R](
      function: Function18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R]
  ): DisposableFunction18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] = {
    new DisposableFunction18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R](
      function: Function19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R]
  ): DisposableFunction19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] = {
    new DisposableFunction19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R](
      function: Function20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R]
  ): DisposableFunction20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] = {
    new DisposableFunction20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19, v20: T20): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R](
      function: Function21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R]
  ): DisposableFunction21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R] = {
    new DisposableFunction21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19, v20: T20, v21: T21): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21)
    }
  }

  implicit def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R](
      function: Function22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R]
  ): DisposableFunction22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R] = {
    new DisposableFunction22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, R] {
      override val identifier = function

      override def apply(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6, v7: T7, v8: T8, v9: T9, v10: T10, v11: T11, v12: T12, v13: T13, v14: T14, v15: T15, v16: T16, v17: T17, v18: T18, v19: T19, v20: T20, v21: T21, v22: T22): R =
        function(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22)
    }
  }
}
