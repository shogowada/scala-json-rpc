package io.github.shogowada.scala.jsonrpc

import scala.concurrent.Future

object Types {
  type Id = Either[String, BigDecimal]

  type JsonSender = (String) => Future[Option[String]]
}
