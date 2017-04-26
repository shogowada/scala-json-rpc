package io.github.shogowada.scala.jsonrpc.example.e2e

import java.util.concurrent.TimeUnit

import org.scalatra.{Ok, ScalatraServlet}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class LogServlet extends ScalatraServlet {
  get("/") {
    val futureResult = JSONRPCModule.loggerAPI.getAllLogs()
        .map(logs => Ok(logs))
    Await.result(futureResult, Duration(1, TimeUnit.MINUTES))
  }
}
