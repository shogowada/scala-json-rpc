package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.util.UUID

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RandomNumberSubjectApiImpl extends RandomNumberSubjectApi {
  private val observers: mutable.Set[String] = mutable.Set()

  override def register(): Future[String] = {
    val observerId = UUID.randomUUID().toString
    Future(observerId)
  }

  override def unregister(observerId: String): Unit = {
  }
}
