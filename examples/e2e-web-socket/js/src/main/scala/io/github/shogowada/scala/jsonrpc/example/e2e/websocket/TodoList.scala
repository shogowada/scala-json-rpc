package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.example.e2e.websocket.TodoList.State
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.ReactClassSpec
import io.github.shogowada.scalajs.reactjs.elements.ReactElement

object TodoList {

  case class Props()

  case class State(todos: Seq[Todo])

}

class TodoList extends ReactClassSpec {
  override type Props = TodoList.Props
  override type State = TodoList.State

  override def getInitialState() = State(Seq())

  override def render(): ReactElement = {
    <.div()(
      <.h2()("TODO List")
    )
  }
}
