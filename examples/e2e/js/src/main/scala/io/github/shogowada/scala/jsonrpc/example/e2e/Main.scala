package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.client.JSONRPCClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scalajs.reactjs.ReactDOM
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.elements.ReactElement
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.JSApp

class App(
    calculatorAPI: CalculatorAPI,
    echoAPI: EchoAPI,
    loggerAPI: LoggerAPI
) {
  def apply(): ReactElement =
    <.div()(
      <((new Calculator(calculatorAPI)) ()).empty,
      <((new Echo(echoAPI)) ()).empty,
      <((new Logger(loggerAPI)) ()).empty
    )
}

object Main extends JSApp {
  override def main(): Unit = {
    val jsonSender: (String) => Future[Option[String]] =
      (json: String) => {
        val NoContentStatus = 204
        dom.ext.Ajax
            .post(url = "/jsonrpc", data = json)
            .map(response => {
              if (response.status == NoContentStatus) {
                None
              } else {
                Option(response.responseText)
              }
            })
      }

    val client = JSONRPCClient(UpickleJsonSerializer(), jsonSender)

    val calculatorAPI = client.createAPI[CalculatorAPI]
    val echoAPI = client.createAPI[EchoAPI]
    val loggerAPI = client.createAPI[LoggerAPI]

    val mountNode = dom.document.getElementById("mount-node")
    ReactDOM.render((new App(calculatorAPI, echoAPI, loggerAPI)) (), mountNode)
  }
}
