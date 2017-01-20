package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scalajs.reactjs.ReactDOM
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.StatelessReactClassSpec
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.JSApp

class App(
    calculatorApi: CalculatorApi,
    echoApi: EchoApi,
    loggerApi: LoggerApi
) extends StatelessReactClassSpec {

  case class Props()

  override def render() = {
    <.div()(
      new Calculator(calculatorApi)(Calculator.Props()),
      new Echo(echoApi)(Echo.Props()),
      new Logger(loggerApi)(Logger.Props())
    )
  }
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

    val client = JsonRpcClient(UpickleJsonSerializer(), jsonSender)

    val calculatorApi = client.createApi[CalculatorApi]
    val echoApi = client.createApi[EchoApi]
    val loggerApi = client.createApi[LoggerApi]

    val mountNode = dom.document.getElementById("mount-node")
    ReactDOM.render(new App(calculatorApi, echoApi, loggerApi), mountNode)
  }
}
