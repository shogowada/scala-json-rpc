package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.DisposableFunction1

import scala.concurrent.Future

case class Todo(id: String, description: String)

object TodoEventTypes {

  val Add = "Add"
  val Remove = "Remove"

}

case class TodoEvent(todo: Todo, eventType: String)

trait TodoRepositoryApi {
  def add(description: String): Future[Todo]

  def remove(id: String): Future[Unit]

  def register(observer: DisposableFunction1[TodoEvent, Future[Unit]]): Future[String]

  def unregister(observerId: String): Future[Unit]
}

