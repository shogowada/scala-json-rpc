package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.util.UUID

import io.github.shogowada.scala.jsonrpc.DisposableFunction1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TodoRepositoryAPIImpl extends TodoRepositoryAPI {

  var todos: Seq[Todo] = Seq()
  var observersById: Map[String, DisposableFunction1[TodoEvent, Future[Unit]]] = Map()

  override def add(description: String): Future[Todo] = this.synchronized {
    val todo = Todo(id = UUID.randomUUID().toString, description)
    todos = todos :+ todo

    notify(TodoEvent(todo, TodoEventTypes.Add))

    Future(todo)
  }

  override def remove(id: String): Future[Unit] = this.synchronized {
    val index = todos.indexWhere(todo => todo.id == id)
    if (index >= 0) {
      val todo = todos(index)
      todos = todos.patch(index, Seq(), 1)
      notify(TodoEvent(todo, TodoEventTypes.Remove))
    }
    Future()
  }

  override def register(observer: DisposableFunction1[TodoEvent, Future[Unit]]): Future[String] = this.synchronized {
    val id = UUID.randomUUID().toString
    observersById = observersById + (id -> observer)

    todos.map(todo => TodoEvent(todo, TodoEventTypes.Add))
        .foreach(todoEvent => notify(id, observer, todoEvent))

    Future(id)
  }

  override def unregister(observerId: String): Future[Unit] = this.synchronized {
    observersById.get(observerId).foreach(observer => {
      observersById = observersById - observerId
      observer.dispose()
    })
    Future()
  }

  private def notify(todoEvent: TodoEvent): Unit = {
    observersById.foreach {
      case (id, observer) => notify(id, observer, todoEvent)
    }
  }

  private def notify(observerId: String, observer: DisposableFunction1[TodoEvent, Future[Unit]], todoEvent: TodoEvent): Unit = {
    observer(todoEvent)
        .failed // Probably connection is lost.
        .foreach(_ => unregister(observerId))
  }
}
