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

    "when I add TODO item" - {
      val newTodoDescription = "Say hello"
      textField(ElementIds.NewTodoDescription).value = newTodoDescription
      clickOn(ElementIds.AddTodo)

      "then it should add the item" in {
        eventually {
          findAll(tagName("li")).exists(element => element.text.contains(newTodoDescription)) should equal(true)
        }
      }

      "and removed the item" - {
        findAll(cssSelector("li>button")).foreach(element => clickOn(element))

        "then it should remove the item" in {
          eventually {
            findAll(tagName("li")) shouldBe empty
          }
        }
      }
    }
  }

  quit()
}
