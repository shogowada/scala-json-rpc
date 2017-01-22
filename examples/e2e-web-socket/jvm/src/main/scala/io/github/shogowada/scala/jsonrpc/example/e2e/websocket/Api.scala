package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.util.UUID

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction1

import scala.concurrent.Future

class TodoRepository(
    todoEventSubject: TodoEventSubject
) extends TodoRepositoryApi {

  var todos: Seq[Todo] = Seq()

  override def add(description: String): Future[Todo] = this.synchronized {
    val todo = Todo(id = UUID.randomUUID().toString, description)
    todos = todos :+ todo

    todoEventSubject.notify(TodoEvent(todo, TodoEventType.Add))

    Future(todo)
  }

  override def remove(id: String): Future[Unit] = this.synchronized {
    val index = todos.indexWhere(todo => todo.id == id)
    if (index >= 0) {
      val todo = todos(index)
      todos = todos.patch(index, Seq(), 1)
      todoEventSubject.notify(TodoEvent(todo, TodoEventType.Remove))
    }
    Future()
  }
}

class TodoEventSubject extends TodoEventSubjectApi {
  var observersById: Map[String, JsonRpcFunction1[TodoEvent, Future[Unit]]] = Map()

  def notify(todoEvent: TodoEvent): Unit = {
    observersById.foreach {
      case (id, observer) => observer(todoEvent).failed.foreach(_ => unregister(id))
    }
  }

  override def register(observer: JsonRpcFunction1[TodoEvent, Future[Unit]]): Future[String] = this.synchronized {
    val id = UUID.randomUUID().toString
    observersById = observersById + (id -> observer)
    Future(id)
  }

  override def unregister(observerId: String): Future[Unit] = this.synchronized {
    observersById.get(observerId).foreach(observer => {
      observersById = observersById - observerId
      observer.dispose()
    })
    Future()
  }
}
