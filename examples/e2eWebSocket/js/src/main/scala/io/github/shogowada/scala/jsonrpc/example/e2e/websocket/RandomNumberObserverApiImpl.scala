package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RandomNumberObserverApiImpl extends RandomNumberObserverApi {
  val id: String = UUID.randomUUID().toString

  override def getId: Future[String] = {
    Future(id)
  }

  override def notify(randomNumber: Int): Unit = {
    println(s"Received random number $randomNumber")
  }
}
