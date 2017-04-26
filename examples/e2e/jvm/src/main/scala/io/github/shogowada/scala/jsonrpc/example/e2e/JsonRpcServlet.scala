package io.github.shogowada.scala.jsonrpc.example.e2e

import org.scalatra._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class JSONRPCServlet extends ScalatraServlet {
  post("/") {
    val server = JSONRPCModule.jsonRPCServer
    val futureResult: Future[ActionResult] = server.receive(request.body).map {
      case Some(responseJson) => Ok(responseJson) // For JSON-RPC request, we return response.
      case None => NoContent() // For JSON-RPC notification, we do not return response.
    }
    Await.result(futureResult, 1.minutes)
  }
}
