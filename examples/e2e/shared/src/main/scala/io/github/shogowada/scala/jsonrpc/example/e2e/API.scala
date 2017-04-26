package io.github.shogowada.scala.jsonrpc.example.e2e

import scala.concurrent.Future

trait CalculatorAPI {
  def add(lhs: Int, rhs: Int): Future[Int]

  def subtract(lhs: Int, rhs: Int): Future[Int]
}

trait EchoAPI {
  def echo(message: String): Future[String]
}

trait LoggerAPI {
  def log(message: String): Unit

  def getAllLogs(): Future[Seq[String]]
}
