package io.github.shogowada.scala.jsonrpc.server

import scala.concurrent.Future

trait JsonReceiver {
  def receive(json: String): Future[Option[String]]
}
