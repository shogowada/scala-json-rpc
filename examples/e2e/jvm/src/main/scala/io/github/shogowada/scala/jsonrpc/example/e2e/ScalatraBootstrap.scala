package io.github.shogowada.scala.jsonrpc.example.e2e

import javax.servlet.ServletContext

import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    context.mount(new JSONRPCServlet, "/jsonrpc/*")
    context.mount(new LogServlet, "/logs/*")
  }
}
