package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.{ReactClassSpec, StatelessReactClassSpec}
import io.github.shogowada.scalajs.reactjs.elements.ReactElement
import io.github.shogowada.scalajs.reactjs.events.InputFormSyntheticEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.Success

object TodoView {

  case class Props(todo: Todo, onRemove: (Todo) => Unit)

}

class TodoView extends StatelessReactClassSpec {
  override type Props = TodoView.Props

  override def render(): ReactElement = {
    <.li(^.key := props.todo.id)(
      props.todo.description,
      <.button(^.onClick := (() => props.onRemove(props.todo)))("Remove")
    ).asReactElement
  }
}

object AddTodoView {

  case class Props(onAdd: (String) => Unit)

  case class State(description: String)

}

class AddTodoView extends ReactClassSpec {

  import AddTodoView._

  override type Props = AddTodoView.Props
  override type State = AddTodoView.State

  override def getInitialState() = State("")

  override def render(): ReactElement = {
    <.div()(
      <.input(
        ^.id := ElementIds.NewTodoDescription,
        ^.value := state.description,
        ^.onChange := onChange
      )(),
      <.button(^.id := ElementIds.AddTodo, ^.onClick := onClick)("Add")
    ).asReactElement
  }

  private val onChange = (event: InputFormSyntheticEvent) => {
    setState(State(event.target.value))
  }

  private val onClick = () => {
    setState(State(""))
    props.onAdd(state.description)
  }
}

object TodoListView {

  case class Props()

  case class State(ready: Boolean, todos: Seq[Todo])

}

class TodoListView(
    todoRepositoryApi: TodoRepositoryApi
) extends ReactClassSpec {

  import TodoListView._

  override type Props = TodoListView.Props
  override type State = TodoListView.State

  override def getInitialState() = State(ready = false, Seq())

  val promisedObserverId: Promise[String] = Promise()

  override def componentDidMount(): Unit = {
    val futureObserverId = todoRepositoryApi.register(onTodoEvent(_))

    futureObserverId.onComplete {
      case Success(_) => setState(_.copy(ready = true))
      case _ =>
    }

    promisedObserverId.completeWith(futureObserverId)
  }

  override def componentWillUnmount(): Unit = {
    promisedObserverId.future.foreach(observerId => {
      todoRepositoryApi.unregister(observerId)
    })
  }

  private def onTodoEvent(event: TodoEvent): Future[Unit] = {
    event.eventType match {
      case TodoEventTypes.Add => setState((prevState: State) => {
        prevState.copy(
          todos = prevState.todos :+ event.todo
        )
      })
      case TodoEventTypes.Remove => setState((prevState: State) => {
        prevState.copy(
          todos = prevState.todos.filterNot(_.id == event.todo.id)
        )
      })
    }
    Future()
  }

  override def render(): ReactElement = {
    <.div()(
      <.h2()("TODO List"),
      <.div(^.id := ElementIds.Ready)(
        state.ready match {
          case true => "Ready!"
          case _ => "Not ready yet..."
        }
      ),
      <.ul()(
        state.todos.map(todo => {
          new TodoView()(TodoView.Props(todo, onRemove = onRemoveTodo))
        })
      ),
      new AddTodoView()(AddTodoView.Props(onAdd = onAddTodo))
    ).asReactElement
  }

  private val onAddTodo = (description: String) => {
    todoRepositoryApi.add(description)
  }: Unit

  private val onRemoveTodo = (todo: Todo) => {
    todoRepositoryApi.remove(todo.id)
  }: Unit
}
