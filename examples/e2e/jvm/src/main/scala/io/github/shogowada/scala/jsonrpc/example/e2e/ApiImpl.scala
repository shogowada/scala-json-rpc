package io.github.shogowada.scala.jsonrpc.example.e2e

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CalculatorAPIImpl extends CalculatorAPI {
  override def add(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs + rhs)
  }

  override def subtract(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs - rhs)
  }
}

class EchoAPIImpl extends EchoAPI {
  override def echo(message: String): Future[String] = {
    Future(message) // It just returns the message as is
  }
}

class LoggerAPIImpl extends LoggerAPI {
  var logs: Seq[String] = Seq()

  override def log(message: String): Unit = this.synchronized {
    logs = logs :+ message
    println(message) // It logs the message
  }

  override def getAllLogs(): Future[Seq[String]] = {
    Future(logs)
  }
}
