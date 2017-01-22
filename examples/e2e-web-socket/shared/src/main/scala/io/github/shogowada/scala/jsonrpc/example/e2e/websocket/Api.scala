package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction1
import io.github.shogowada.scala.jsonrpc.example.e2e.websocket.TodoEventType.TodoEventType

import scala.concurrent.Future

case class Todo(id: String, description: String)

trait TodoRepositoryApi {
  def add(description: String): Future[Todo]

  def remove(id: String): Future[Unit]
}

object TodoEventType {

  sealed trait TodoEventType {
    val description: String
  }

  case object Add extends TodoEventType {
    val description = "Add"
  }

  case object Remove extends TodoEventType {
    val description = "Remove"
  }

}

case class TodoEvent(todo: Todo, eventType: TodoEventType)

trait TodoEventSubjectApi {
  def register(observer: JsonRpcFunction1[TodoEvent, Future[Unit]]): Future[String]

  def unregister(observerId: String): Future[Unit]
}
