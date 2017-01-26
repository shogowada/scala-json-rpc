package io.github.shogowada.scala.jsonrpc.example.e2e.websocket.integrationtest

import io.github.shogowada.scala.jsonrpc.example.e2e.websocket.ElementIds
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.Firefox
import org.scalatest.{Matchers, path}

class TodoListTest extends path.FreeSpec
    with Firefox
    with Eventually
    with Matchers {

  "given I am on TODO list" - {
    go to Target.url

    "then it should say it is ready" in {
      eventually {
        find(id(ElementIds.Ready)).get.text should equal("Ready!")
      }
    }

    clearTodos()

    "when I add TODO item" - {
      val newTodoDescription = "Say hello"

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
