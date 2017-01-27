package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.ReactClassSpec
import io.github.shogowada.scalajs.reactjs.events.InputFormSyntheticEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Echo {

  case class Props()

  case class State(text: String, echoedText: Option[String])

}

class Echo(echoApi: EchoApi) extends ReactClassSpec {

  type Props = Echo.Props
  type State = Echo.State

  override def getInitialState() = {
    Echo.State(text = "", echoedText = Some(""))
  }

  override def render() = {
    <.div()(
      <.h2()("Echo"),
      <.label(^.`for` := ElementIds.EchoText)("I say:"),
      <.input(
        ^.id := ElementIds.EchoText,
        ^.value := state.text,
        ^.onChange := onChange
      )(),
      <.label(^.`for` := ElementIds.EchoEchoedText)("Server says:"),
      <.span(^.id := ElementIds.EchoEchoedText)(state.echoedText.getOrElse(""))
    ).asReactElement
  }

  private val onChange = (event: InputFormSyntheticEvent) => {
    val text = event.target.value

    setState(_.copy(
      text = text,
      echoedText = None
    ))

    echoApi.echo(text).onComplete {
      case Success(echoedText) if state.text == text => setState(_.copy(echoedText = Some(echoedText)))
      case _ =>
    }
  }
}
