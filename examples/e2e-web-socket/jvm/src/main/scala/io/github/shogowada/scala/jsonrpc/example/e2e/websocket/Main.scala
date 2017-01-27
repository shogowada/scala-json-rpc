package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext

object Main {
  def main(args: Array[String]): Unit = {
    val port = System.getProperty("port", "8080").toInt
    val server = new Server(port)

    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase(
      Resource.newResource(ClassLoader.getSystemResource("public"))
          .getURI
          .toASCIIString
    )
    context.addServlet(classOf[DefaultServlet], "/")
    context.addServlet(classOf[JsonRpcWebSocketServlet], "/jsonrpc")

    server.setHandler(context)

    server.start()
    server.join()
  }
}
