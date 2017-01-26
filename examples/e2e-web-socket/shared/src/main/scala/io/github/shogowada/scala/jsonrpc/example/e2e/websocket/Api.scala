package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction1

import scala.concurrent.Future

case class Todo(id: String, description: String)

trait TodoRepositoryApi {
  def add(description: String): Future[Todo]

  def remove(id: String): Future[Unit]
}

object TodoEventTypes {

  val Add = "Add"
  val Remove = "Remove"

}

case class TodoEvent(todo: Todo, eventType: String)

trait TodoEventSubjectApi {
  def register(observer: JsonRpcFunction1[TodoEvent, Future[Unit]]): Future[String]

  def unregister(observerId: String): Future[Unit]
}