package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.{JsonRpcServer, JsonRpcServerBuilder}

import scala.concurrent.ExecutionContext.Implicits.global

object JsonRpcModule {

  import com.softwaremill.macwire._

  lazy val calculatorApi = wire[CalculatorApiImpl]
  lazy val echoApi = wire[EchoApiImpl]
  lazy val loggerApi = wire[LoggerApiImpl]

  lazy val jsonRpcServer: JsonRpcServer[UpickleJsonSerializer] = {
    val builder = JsonRpcServerBuilder(UpickleJsonSerializer())
    builder.bindApi[CalculatorApi](calculatorApi)
    builder.bindApi[EchoApi](echoApi)
    builder.bindApi[LoggerApi](loggerApi)
    builder.build
  }
}
