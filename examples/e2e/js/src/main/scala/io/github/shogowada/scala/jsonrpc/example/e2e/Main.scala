package io.github.shogowada.scala.jsonrpc.example.e2e

import io.github.shogowada.scala.jsonrpc.client.JsonRpcClient
import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer
import io.github.shogowada.scalajs.reactjs.ReactDOM
import io.github.shogowada.scalajs.reactjs.VirtualDOM._
import io.github.shogowada.scalajs.reactjs.classes.specs.ReactClassSpec
import io.github.shogowada.scalajs.reactjs.elements.ReactHTMLInputElement
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.JSApp
import scala.util.Success

object Main extends JSApp {
  override def main(): Unit = {
    object Calculator {

      case class Props()

      case class State(lhs: Int, rhs: Int, added: Int, subtracted: Int)

    }
    class Calculator extends ReactClassSpec {
      type Props = Calculator.Props
      type State = Calculator.State

      private var lhsInputElement: ReactHTMLInputElement = _
      private var rhsInputElement: ReactHTMLInputElement = _

      override def getInitialState() = {
        Calculator.State(0, 0, 0, 0)
      }

      override def render() = {
        <.div()(
          <.form()(
            <.input(
              ^.id := "calculator-lhs",
              ^.ref := ((element: ReactHTMLInputElement) => {
                lhsInputElement = element
              }),
              ^.onChange := onChange
            )(),
            <.input(
              ^.id := "calculator-rhs",
              ^.ref := ((element: ReactHTMLInputElement) => {
                rhsInputElement = element
              }),
              ^.onChange := onChange
            )(),
            <.button(
              ^.id := "calculator-calculate",
              ^.onSubmit := onSubmit
            )("Calculate")
          ),
          <.div(^.id := "calculator-added")(state.added),
          <.div(^.id := "calculator-subtracted")(state.subtracted)
        )
      }

      private val onChange = () => {
        setState(previous => previous.copy(
          lhs = lhsInputElement.value.toInt,
          rhs = rhsInputElement.value.toInt
        ))
      }

      private val onSubmit = () => {
      }
    }
    val mountNode = dom.document.getElementById("mount-node")
    ReactDOM.render(new Calculator(), mountNode)

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

    loggerApi.log("This is the beginning of my example.")

    calculatorApi.add(1, 2).onComplete {
      case Success(result) => println(s"1 + 2 = $result")
      case _ =>
    }

    calculatorApi.subtract(1, 2).onComplete {
      case Success(result) => println(s"1 - 2 = $result")
      case _ =>
    }

    echoApi.echo("Hello, World!").onComplete {
      case Success(result) => println(s"""You said "$result"""")
      case _ =>
    }

    loggerApi.log("This is the end of my example.")
  }
}
