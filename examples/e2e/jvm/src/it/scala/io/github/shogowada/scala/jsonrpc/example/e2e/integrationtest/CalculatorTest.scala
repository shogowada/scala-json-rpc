package io.github.shogowada.scala.jsonrpc.example.e2e.integrationtest

import io.github.shogowada.scala.jsonrpc.example.e2e.ElementIds
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.Firefox
import org.scalatest.{Matchers, path}

class CalculatorTest extends path.FreeSpec
    with Eventually
    with Matchers
    with Firefox {

  "given I am on the calculator page" - {
    go to TargetController.url

    "then it should display the page" in {
      eventually {
        find(ElementIds.CalculatorCalculate) shouldBe defined
      }
    }

    "and I entered 2 and 3" - {
      textField(ElementIds.CalculatorLhs).value = "2"
      textField(ElementIds.CalculatorRhs).value = "3"

      "when I clicked on calculate button" - {
        clickOn(ElementIds.CalculatorCalculate)

        "then it should add the numbers" in {
          eventually {
            find(ElementIds.CalculatorAdded).get.text should equal("2 + 3 = 5")
          }
        }

        "then it should subtract the numbers" in {
          eventually {
            find(ElementIds.CalculatorSubtracted).get.text should equal("2 - 3 = -1")
          }
        }
      }
    }
  }

  close()
}
