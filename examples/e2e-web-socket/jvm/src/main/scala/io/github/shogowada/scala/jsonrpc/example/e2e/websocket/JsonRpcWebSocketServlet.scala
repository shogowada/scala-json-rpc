package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import javax.servlet.annotation.WebServlet

import org.eclipse.jetty.websocket.servlet.{WebSocketServlet, WebSocketServletFactory}

@WebServlet(name = "JSON-RPC WebSocket servlet", urlPatterns = Array("/jsonrpc"))
class JsonRpcWebSocketServlet extends WebSocketServlet {
  override def configure(factory: WebSocketServletFactory): Unit = {
    factory.register(classOf[JsonRpcWebSocket])
  }
}
