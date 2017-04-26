package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scalajs.reactjs.React
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.events.{FormSyntheticEvent, SyntheticEvent}
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Logger {
  case class State(log: String, logs: Seq[String])

  type Self = React.Self[Unit, State]
}

class Logger(loggerAPI: LoggerAPI) {

  import Logger._

  def apply() = reactClass

  private lazy val reactClass = React.createClass[Unit, State](
    getInitialState = (self) => State("", Seq()),
    render = (self) =>
      <.div()(
        <.h2()("Logger"),
        <.form(^.onSubmit := onLog(self))(
          <.input(
            ^.id := ElementIds.LoggerLogText,
            ^.value := self.state.log,
            ^.onChange := onChange(self)
          )(),
          <.button(
            ^.id := ElementIds.LoggerLog,
            ^.`type` := "submit"
          )("Log")
        ),
        <.form(^.onSubmit := onGetLogs(self))(
          <.button(
            ^.id := ElementIds.LoggerGetLogs,
            ^.`type` := "submit"
          )("Get Logs")
        ),
        <.div(^.id := ElementIds.LoggerLogs)(
          self.state.logs.map(log => {
            <.div()(log)
          })
        )
      ).asReactElement
  )

  private def onChange(self: Self) =
    (event: FormSyntheticEvent[HTMLInputElement]) => {
      val log = event.target.value

      self.setState(_.copy(log = log))
    }

  private def onLog(self: Self) =
    (event: SyntheticEvent) => {
      event.preventDefault()

      loggerAPI.log(self.state.log)

      self.setState(_.copy(log = ""))
    }

  private def onGetLogs(self: Self) =
    (event: SyntheticEvent) => {
      event.preventDefault()

      loggerAPI.getAllLogs().onComplete {
        case Success(logs) => self.setState(_.copy(logs = logs))
        case _ =>
      }
    }
}
