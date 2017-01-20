package io.github.shogowada.scala.jsonrpc.example.e2e

import org.scalatra.{Ok, ScalatraServlet}

class LogServlet extends ScalatraServlet {
  get("/") {
    val logRepository = JsonRpcModule.logRepository
    Ok(logRepository.all)
  }
}
