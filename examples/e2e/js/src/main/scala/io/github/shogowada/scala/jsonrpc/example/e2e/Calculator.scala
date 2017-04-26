package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scalajs.reactjs.React
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.events.{FormSyntheticEvent, SyntheticEvent}
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Calculator {
  case class State(lhs: Int, rhs: Int, added: Option[Int], subtracted: Option[Int])

  type Self = React.Self[Unit, State]
}

class Calculator(calculatorAPI: CalculatorAPI) {

  import Calculator._

  def apply() = reactClass

  private lazy val reactClass = React.createClass[Unit, State](
    getInitialState = (self) => Calculator.State(0, 0, None, None),
    render = (self) =>
      <.div()(
        <.h2()("Calculator"),
        <.form(^.onSubmit := onSubmit(self))(
          <.input(
            ^.id := ElementIds.CalculatorLhs,
            ^.onChange := onLhsChange(self),
            ^.value := self.state.lhs
          )(),
          <.input(
            ^.id := ElementIds.CalculatorRhs,
            ^.onChange := onRhsChange(self),
            ^.value := self.state.rhs
          )(),
          <.button(
            ^.id := ElementIds.CalculatorCalculate,
            ^.`type` := "submit"
          )("Calculate")
        ),
        <.div(^.id := ElementIds.CalculatorAdded)(
          s"${self.state.lhs} + ${self.state.rhs} = ${self.state.added.getOrElse("?")}"
        ),
        <.div(^.id := ElementIds.CalculatorSubtracted)(
          s"${self.state.lhs} - ${self.state.rhs} = ${self.state.subtracted.getOrElse("?")}"
        )
      ).asReactElement
  )

  private def onLhsChange(self: Self) =
    (event: FormSyntheticEvent[HTMLInputElement]) => {
      val value = event.target.value
      self.setState(_.copy(
        lhs = value.toInt,
        added = None,
        subtracted = None
      ))
    }

  private def onRhsChange(self: Self) =
    (event: FormSyntheticEvent[HTMLInputElement]) => {
      val value = event.target.value
      self.setState(_.copy(
        rhs = value.toInt,
        added = None,
        subtracted = None
      ))
    }

  private def onSubmit(self: Self) =
    (event: SyntheticEvent) => {
      event.preventDefault()

      val lhs = self.state.lhs
      val rhs = self.state.rhs

      calculatorAPI.add(lhs, rhs).onComplete {
        case Success(added) if lhs == self.state.lhs && rhs == self.state.rhs => {
          self.setState(_.copy(added = Some(added)))
        }
        case _ =>
      }

      calculatorAPI.subtract(lhs, rhs).onComplete {
        case Success(subtracted) if lhs == self.state.lhs && rhs == self.state.rhs => {
          self.setState(_.copy(subtracted = Some(subtracted)))
        }
        case _ =>
      }
    }
}
