package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scala.jsonrpc.server.{JsonRpcServer, JsonRpcServerBuilder}

import scala.concurrent.ExecutionContext.Implicits.global

object JsonRpcModule {
  lazy val jsonRpcServer: JsonRpcServer[UpickleJsonSerializer] = {
    val builder = JsonRpcServerBuilder(UpickleJsonSerializer())
    builder.bindApi[CalculatorApi](new CalculatorApiImpl)
    builder.bindApi[EchoApi](new EchoApiImpl)
    builder.bindApi[LoggerApi](new LoggerApiImpl)
    builder.build
  }
}
