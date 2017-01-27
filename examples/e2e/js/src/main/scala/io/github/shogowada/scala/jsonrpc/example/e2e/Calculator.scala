package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.ReactClassSpec
import io.github.shogowada.scalajs.reactjs.elements.ReactHTMLInputElement
import io.github.shogowada.scalajs.reactjs.events.SyntheticEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Calculator {

  case class Props()

  case class State(lhs: Int, rhs: Int, added: Option[Int], subtracted: Option[Int])

}

class Calculator(calculatorApi: CalculatorApi) extends ReactClassSpec {
  type Props = Calculator.Props
  type State = Calculator.State

  private var lhsElement: ReactHTMLInputElement = _
  private var rhsElement: ReactHTMLInputElement = _

  override def getInitialState() = {
    Calculator.State(0, 0, None, None)
  }

  override def render() = {
    <.div()(
      <.h2()("Calculator"),
      <.form(^.onSubmit := onSubmit)(
        <.input(
          ^.id := ElementIds.CalculatorLhs,
          ^.ref := ((element: ReactHTMLInputElement) => {
            lhsElement = element
          }),
          ^.onChange := onChange,
          ^.value := state.lhs
        )(),
        <.input(
          ^.id := ElementIds.CalculatorRhs,
          ^.ref := ((element: ReactHTMLInputElement) => {
            rhsElement = element
          }),
          ^.onChange := onChange,
          ^.value := state.rhs
        )(),
        <.button(
          ^.id := ElementIds.CalculatorCalculate,
          ^.`type` := "submit"
        )("Calculate")
      ),
      <.div(^.id := ElementIds.CalculatorAdded)(
        s"${state.lhs} + ${state.rhs} = ${state.added.getOrElse("?")}"
      ),
      <.div(^.id := ElementIds.CalculatorSubtracted)(
        s"${state.lhs} - ${state.rhs} = ${state.subtracted.getOrElse("?")}"
      )
    ).asReactElement
  }

  private val onChange = () => {
    setState(state.copy(
      lhs = lhsElement.value.toInt,
      rhs = rhsElement.value.toInt,
      added = None,
      subtracted = None
    ))
  }

  private val onSubmit = (event: SyntheticEvent) => {
    event.preventDefault()

    val lhs = state.lhs
    val rhs = state.rhs

    calculatorApi.add(lhs, rhs).onComplete {
      case Success(added) if lhs == state.lhs && rhs == state.rhs => {
        setState(_.copy(added = Some(added)))
      }
      case _ =>
    }

    calculatorApi.subtract(lhs, rhs).onComplete {
      case Success(subtracted) if lhs == state.lhs && rhs == state.rhs => {
        setState(_.copy(subtracted = Some(subtracted)))
      }
      case _ =>
    }
  }
}
