package io.github.shogowada.scala.jsonrpc.example.e2e.websocket.integrationtest

import io.github.shogowada.scala.jsonrpc.example.e2e.websocket.ElementIds
import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.Firefox
import org.scalatest.{Matchers, path}

class TodoListTest extends path.FreeSpec
    with Firefox
    with Eventually
    with Matchers {

  def waitFor[T](condition: ExpectedCondition[T])(implicit webDriver: WebDriver): T = {
    new WebDriverWait(webDriver, 3).until[T](condition)
  }

  "given I am on TODO list" - {
    go to Target.url

    waitFor(ExpectedConditions.textToBe(By.id(ElementIds.Ready), "Ready!"))

    clearTodos()

    "when I add TODO item" - {
      val newTodoDescription = "Say hello"

      waitFor(ExpectedConditions.visibilityOfElementLocated(By.id(ElementIds.NewTodoDescription)))
      textField(id(ElementIds.NewTodoDescription)).value = newTodoDescription
      clickOn(id(ElementIds.AddTodo))

      "then it should add the item" in {
        verifyTodoExists(newTodoDescription)
      }

      "and I reload the page" - {
        reloadPage()

        "then it should still show the item" in {
          verifyTodoExists(newTodoDescription)
        }
      }

      "and removed the item" - {
        find(cssSelector("li>button")).foreach(element => clickOn(element))

        "then it should remove the item" in {
          eventually {
            findAll(tagName("li")) shouldBe empty
          }
        }
      }
    }
  }

  def clearTodos(): Unit = {
    findAll(cssSelector("li>button")).foreach(element => clickOn(element))
  }

  def verifyTodoExists(description: String): Unit = {
    eventually {
      findAll(tagName("li")).exists(element => element.text.contains(description)) should equal(true)
    }
  }

  quit()
}
