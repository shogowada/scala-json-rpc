package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.ReactClassSpec
import io.github.shogowada.scalajs.reactjs.events.{InputFormSyntheticEvent, SyntheticEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Logger {

  case class Props()

  case class State(log: String, logs: Seq[String])

}

class Logger(loggerApi: LoggerApi) extends ReactClassSpec {
  type Props = Logger.Props
  type State = Logger.State

  override def getInitialState() = {
    Logger.State("", Seq())
  }

  override def render() = {
    <.div()(
      <.h2()("Logger"),
      <.form(^.onSubmit := onLog)(
        <.input(
          ^.id := ElementIds.LoggerLogText,
          ^.value := state.log,
          ^.onChange := onChange
        )(),
        <.button(
          ^.id := ElementIds.LoggerLog,
          ^.`type` := "submit"
        )("Log")
      ),
      <.form(^.onSubmit := onGetLogs)(
        <.button(
          ^.id := ElementIds.LoggerGetLogs,
          ^.`type` := "submit"
        )("Get Logs")
      ),
      <.div(^.id := ElementIds.LoggerLogs)(
        state.logs.map(log => {
          <.div()(log)
        })
      )
    ).asReactElement
  }

  private val onChange = (event: InputFormSyntheticEvent) => {
    val log = event.target.value

    setState(_.copy(log = log))
  }

  private val onLog = (event: SyntheticEvent) => {
    event.preventDefault()

    loggerApi.log(state.log)

    setState(_.copy(log = ""))
  }

  private val onGetLogs = (event: SyntheticEvent) => {
    event.preventDefault()

    loggerApi.getAllLogs().onComplete {
      case Success(logs) => setState(_.copy(logs = logs))
      case _ =>
    }
  }
}
