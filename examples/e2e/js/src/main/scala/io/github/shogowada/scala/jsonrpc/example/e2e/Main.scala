package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClientBuilder
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js.JSApp
import scala.util.Success

object Main extends JSApp {
  override def main(): Unit = {
    val jsonSender: (String) => Future[Option[String]] =
      (json: String) => {
        dom.ext.Ajax
            .post(url = "/jsonrpc", data = json)
            .map(response => {
              if (response.status == 204) {
                None
              } else {
                Option(response.responseText)
              }
            })
      }

    val clientBuilder = JsonRpcClientBuilder(UpickleJsonSerializer(), jsonSender)

    val client = clientBuilder.build

    val calculatorApi = client.createApi[CalculatorApi]
    val echoApi = client.createApi[EchoApi]
    val loggerApi = client.createApi[LoggerApi]

    loggerApi.log("This is the beginning of my example.")

    calculatorApi.add(1, 2).onComplete {
      case Success(result) => println(s"1 + 2 = $result")
    }

    calculatorApi.subtract(1, 2).onComplete {
      case Success(result) => println(s"1 - 2 = $result")
    }

    echoApi.echo("Hello, World!").onComplete {
      case Success(result) => println(s"You said \"$result\"")
    }

    loggerApi.log("This is the end of my example.")
  }
}
