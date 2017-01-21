package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction1

import scala.concurrent.Future

case class Todo(id: String, description: String)

trait TodoRepositoryApi {
  def add(description: String): Future[Todo]

  def remove(id: String): Future[Unit]
}

case class TodoEvent(todo: Todo, description: String)

trait TodoEventSubjectApi {
  def register(observer: JsonRpcFunction1[TodoEvent, Future[Unit]]): Future[String]

  def unregister(registrationId: String)
}

trait RandomNumberSubjectApi {
  def register(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit

  def unregister(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit
}
