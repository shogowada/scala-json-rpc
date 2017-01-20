package io.github.shogowada.scala.jsonrpc.example.e2e

class LogRepository {

  var logs: Seq[String] = Seq()

  def add(log: String): Unit = this.synchronized {
    logs = logs :+ log
  }

  def all: Seq[String] = {
    logs
  }
}
