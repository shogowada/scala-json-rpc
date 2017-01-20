package io.github.shogowada.scala.jsonrpc.example.e2e.integrationtest

import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.Firefox
import org.scalatest.{Matchers, path}

class CalculatorApiTest extends path.FreeSpec
    with Eventually
    with Matchers
    with Firefox {

  val target = TargetController.target

  "given I am on the home page" - {
    go to TargetController.url

    "then it should display the page" in {
      eventually {
        find("calculator-calculate") shouldBe defined
      }
    }

    "and I entered 2 and 3" - {
      textField("calculator-lhs").value = "2"
      textField("calculator-rhs").value = "3"

      "when I clicked on calculate button" - {
        clickOn("calculator-calculate")

        "then it should add the numbers" in {
          eventually {
            find("calculator-added").get.text should equal("5")
          }
        }

        "then it should subtract the numbers" in {
          eventually {
            find("calculator-subtracted").get.text should equal("-1")
          }
        }
      }
    }
  }

  close()
}
