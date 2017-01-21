package io.github.shogowada.scala.jsonrpc.example.e2e.integrationtest

import io.github.shogowada.scala.jsonrpc.example.e2e.ElementIds
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.Firefox
import org.scalatest.{Matchers, path}

class LoggerTest extends path.FreeSpec
    with Matchers
    with Eventually
    with Firefox {

  "given I am on logger page" - {
    go to TargetController.url

    "when I log something" - {
      val log = "Ah, looks like something happened?"

      textField(ElementIds.LoggerLogText).value = log
      clickOn(ElementIds.LoggerLog)

      "and I get logs" - {
        clickOn(ElementIds.LoggerGetLogs)

        "then it should log the text" in {
          eventually {
            find(ElementIds.LoggerLogs).get.text should equal(log)
          }
        }
      }
    }
  }

  close()
}
