package io.github.shogowada.scala.jsonrpc.example.e2e

import org.scalatra._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class JsonRpcServlet extends ScalatraServlet {
  post("/") {
    val server = JsonRpcModule.jsonRpcServer
    val futureResult: Future[ActionResult] = server.receive(request.body).map {
      case Some(responseJson) => Ok(responseJson)
      case None => NoContent()
    }
    Await.result(futureResult, 1 minutes)
  }
}
