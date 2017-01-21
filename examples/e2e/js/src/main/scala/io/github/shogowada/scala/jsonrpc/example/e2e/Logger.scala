package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.ReactClassSpec
import io.github.shogowada.scalajs.reactjs.events.{InputElementSyntheticEvent, SyntheticEvent}

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
    )
  }

  private val onChange = (event: InputElementSyntheticEvent) => {
    val log = event.target.value

    setState(state.copy(log = log))
  }

  private val onLog = (event: SyntheticEvent) => {
    event.preventDefault()

    loggerApi.log(state.log)

    setState(state.copy(log = ""))
  }

  private val onGetLogs = (event: SyntheticEvent) => {
    event.preventDefault()

    loggerApi.getAllLogs().onComplete {
      case Success(logs) => setState(state.copy(logs = logs))
      case _ =>
    }
  }
}
