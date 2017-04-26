package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.JsonRpcServer

import scala.concurrent.ExecutionContext.Implicits.global

object JsonRpcModule {
  lazy val loggerApi: LoggerAPI = new LoggerAPIImpl

  lazy val jsonRpcServer: JsonRpcServer[UpickleJsonSerializer] = {
    val server = JsonRpcServer(UpickleJsonSerializer())
    server.bindAPI[CalculatorAPI](new CalculatorAPIImpl)
    server.bindAPI[EchoAPI](new EchoAPIImpl)
    server.bindAPI[LoggerAPI](loggerApi)
    server
  }
}
