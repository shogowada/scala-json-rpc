package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

import scala.concurrent.ExecutionContext.Implicits.global

object JsonRpcModule {
  lazy val logRepository = new LogRepository

  lazy val jsonRpcServer: JsonRpcServer[UpickleJsonSerializer] = {
    val server = JsonRpcServer(UpickleJsonSerializer())
    server.bindApi[CalculatorApi](new CalculatorApiImpl)
    server.bindApi[EchoApi](new EchoApiImpl)
    server.bindApi[LoggerApi](new LoggerApiImpl)
    server
  }
}
