package io.github.shogowada.scala.jsonrpc.example.e2e

import scala.concurrent.Future

trait CalculatorApi {
  def add(lhs: Int, rhs: Int): Future[Int]

  def subtract(lhs: Int, rhs: Int): Future[Int]
}

trait EchoApi {
  def echo(message: String): Future[String]
}

trait LoggerApi {
  def log(message: String): Unit

  def getAllLogs(): Future[Seq[String]]
}
