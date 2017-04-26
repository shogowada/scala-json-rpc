package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scalajs.reactjs.React
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.elements.ReactElement
import io.github.shogowada.scalajs.reactjs.events.FormSyntheticEvent
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.Success

object TodoView {
  case class Props(todo: Todo, onRemove: (Todo) => Unit)

  def apply(props: TodoView.Props): ReactElement = {
    <.li(^.key := props.todo.id)(
      props.todo.description,
      <.button(^.onClick := (() => props.onRemove(props.todo)))("Remove")
    ).asReactElement
  }
}

object AddTodoView {
  case class Props(onAdd: (String) => Unit)

  case class State(description: String)

  type Self = React.Self[Props, State]
}

class AddTodoView {

  import AddTodoView._

  def apply() = reactClass

  private lazy val reactClass = React.createClass[Props, State](
    getInitialState = (self) => State(""),
    render = (self) =>
      <.div()(
        <.input(
          ^.id := ElementIds.NewTodoDescription,
          ^.value := self.state.description,
          ^.onChange := onChange(self)
        )(),
        <.button(^.id := ElementIds.AddTodo, ^.onClick := onClick(self))("Add")
      ).asReactElement
  )

  private def onChange(self: Self) =
    (event: FormSyntheticEvent[HTMLInputElement]) => {
      self.setState(State(event.target.value))
    }

  private def onClick(self: Self) =
    () => {
      self.setState(State(""))
      self.props.wrapped.onAdd(self.state.description)
    }
}

object TodoListView {
  case class State(ready: Boolean, todos: Seq[Todo])

  type Self = React.Self[Unit, State]
}

class TodoListView(
    todoRepositoryAPI: TodoRepositoryAPI
) {

  import TodoListView._

  def apply() = reactClass

  private lazy val reactClass = React.createClass[Unit, State](
    getInitialState = (self) => State(ready = false, Seq()),
    componentDidMount = (self) => {
      val futureObserverId = todoRepositoryAPI.register(onTodoEvent(self, _))

      futureObserverId.onComplete {
        case Success(_) => self.setState(_.copy(ready = true))
        case _ =>
      }

      promisedObserverId.completeWith(futureObserverId)
    },
    componentWillUnmount = (self) => {
      promisedObserverId.future.foreach(observerId => {
        todoRepositoryAPI.unregister(observerId)
      })
    },
    render = (self) =>
      <.div()(
        <.h2()("TODO List"),
        <.div(^.id := ElementIds.Ready)(
          self.state.ready match {
            case true => "Ready!"
            case _ => "Not ready yet..."
          }
        ),
        <.ul()(
          self.state.todos.map(todo => {
            TodoView(TodoView.Props(todo, onRemove = onRemoveTodo))
          })
        ),
        <((new AddTodoView()) ())(^.wrapped := AddTodoView.Props(onAdd = onAddTodo))()
      ).asReactElement
  )

  val promisedObserverId: Promise[String] = Promise()

  private def onTodoEvent(self: Self, event: TodoEvent): Future[Unit] = {
    event.eventType match {
      case TodoEventTypes.Add => self.setState((prevState: State) => {
        prevState.copy(
          todos = prevState.todos :+ event.todo
        )
      })
      case TodoEventTypes.Remove => self.setState((prevState: State) => {
        prevState.copy(
          todos = prevState.todos.filterNot(_.id == event.todo.id)
        )
      })
    }
    Future()
  }

  private val onAddTodo = (description: String) => {
    todoRepositoryAPI.add(description)
  }: Unit

  private val onRemoveTodo = (todo: Todo) => {
    todoRepositoryAPI.remove(todo.id)
  }: Unit
}
