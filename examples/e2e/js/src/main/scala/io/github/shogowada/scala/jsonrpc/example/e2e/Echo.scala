package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scalajs.reactjs.React
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.events.FormSyntheticEvent
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Echo {
  case class State(text: String, echoedText: Option[String])

  type Self = React.Self[Unit, State]
}

class Echo(echoApi: EchoApi) {

  import Echo._

  def apply() = reactClass

  private lazy val reactClass = React.createClass[Unit, State](
    getInitialState = (self) => State(text = "", echoedText = Some("")),
    render = (self) =>
      <.div()(
        <.h2()("Echo"),
        <.label(^.`for` := ElementIds.EchoText)("I say:"),
        <.input(
          ^.id := ElementIds.EchoText,
          ^.value := self.state.text,
          ^.onChange := onChange(self)
        )(),
        <.label(^.`for` := ElementIds.EchoEchoedText)("Server says:"),
        <.span(^.id := ElementIds.EchoEchoedText)(self.state.echoedText.getOrElse(""))
      ).asReactElement
  )

  private def onChange(self: Self) =
    (event: FormSyntheticEvent[HTMLInputElement]) => {
      val text = event.target.value

      self.setState(_.copy(
        text = text,
        echoedText = None
      ))

      echoApi.echo(text).onComplete {
        case Success(echoedText) if self.state.text == text => self.setState(_.copy(echoedText = Some(echoedText)))
        case _ =>
      }
    }
}
