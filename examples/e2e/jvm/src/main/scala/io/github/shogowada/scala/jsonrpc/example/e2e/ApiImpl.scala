package io.github.shogowada.scala.jsonrpc.example.e2e

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CalculatorApiImpl extends CalculatorApi {
  override def add(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs + rhs)
  }

  override def subtract(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs - rhs)
  }
}

class EchoApiImpl extends EchoApi {
  override def echo(message: String): Future[String] = {
    Future(message)
  }
}

class LoggerApiImpl extends LoggerApi {
  override def log(message: String): Unit = {
    println(message)
  }
}
