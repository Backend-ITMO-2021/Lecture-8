import TestUtils.withServer
import ru.ifmo.backend_2021.{Message, RedditApplication}
import utest.{TestSuite, Tests, test}
import castor.Context.Simple.global
import cask.util.Logger.Console._

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf

object RedditTest extends TestSuite {
  val tests: Tests = Tests {
    test("success") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }
      val success = requests.get(host)

      assert(success.text().contains("Reddit: Swain is mad :("))
      assert(success.text().contains("ventus976"))
      assert(success.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success.text().contains("XimbalaHu3"))
      assert(success.text().contains("Exactly, both is fine but do pick one."))
      assert(success.statusCode == 200)

      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("ventus976"))
      assert(wsMsg.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg.contains("XimbalaHu3"))
      assert(wsMsg.contains("Exactly, both is fine but do pick one."))

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg2.contains("ventus976"))
      assert(wsMsg2.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg2.contains("XimbalaHu3"))
      assert(wsMsg2.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg2.contains("ilya"))
      assert(wsMsg2.contains("Test Message!"))

      val success2 = requests.get(host)
      assert(success2.text().contains("ventus976"))
      assert(success2.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success2.text().contains("XimbalaHu3"))
      assert(success2.text().contains("Exactly, both is fine but do pick one."))
      assert(success2.text().contains("ilya"))
      assert(success2.text().contains("Test Message!"))
      assert(success2.statusCode == 200)

      // numerating
      assert(success.text().contains("#1"))
      assert(success.text().contains("#2"))

      // cascade & reply
      wsPromise = scala.concurrent.Promise[String]
      requests.post(host, data = ujson.Obj("name" -> "test", "msg" -> "Test Message!", "replyTo" -> "1"))

      val cascadeSuccess = requests.get(host)
      assert(cascadeSuccess.text().contains("    </span>#2 <b>test</b>"))

      // api
      val messagesResponse = requests.get(s"$host/messages")
      val messages = ujson.read(messagesResponse)("messages")
      assert(messages.arr.length == 4)
      assert(messages(0).toString().contains("\"username\":\"ventus976\""))
      assert(messages(1).toString().contains("\"username\":\"XimbalaHu3\""))
      assert(messages(2).toString().contains("\"username\":\"ilya\""))
      assert(messages(3).toString().contains("\"username\":\"test\""))

      val userMessageResponse = requests.get(s"$host/messages/test")
      val userMessages = ujson.read(userMessageResponse)("messages")
      assert(userMessages.arr.length == 1)
      assert(userMessages.arr(0).toString().contains("Test Message!"))

      wsPromise = scala.concurrent.Promise[String]
      val addMessageResponse = ujson.read(requests.post(s"$host/messages", data = ujson.Obj("username" -> "test2", "message" -> "Test message 2!")))
      assert(addMessageResponse("success") == ujson.True)
      assert(addMessageResponse("err") == ujson.Str(""))

      val wsMsg4 = Await.result(wsPromise.future, Inf)
      assert(wsMsg4.contains("Test message 2!"))

      // filter messages
      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(cask.Ws.Text("test"))

      val wsMsg3 = Await.result(wsPromise.future, Inf)

      assert(wsMsg3.contains("#2"))
      assert(wsMsg3.contains("test"))
      assert(!wsMsg3.contains("ventus976"))
      assert(!wsMsg3.contains("ilya"))
      assert(!wsMsg3.contains("XimbalaHu3"))
    }

    test("failure") - withServer(RedditApplication) { host =>
      val response1 = requests.post(host, data = ujson.Obj("name" -> "ilya"), check = false)
      assert(response1.statusCode == 400)
      val response2 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(host, data = ujson.Obj("name" -> "", "msg" -> "Test Message!"))
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(host, data = ujson.Obj("name" -> "123#123", "msg" -> "Test Message!"))
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )

      val response5 = requests.post(host, data = ujson.Obj("name" -> "123123", "msg" -> "Test Message!", "replyTo" -> "0"))
      assert(
        ujson.read(response5) ==
          ujson.Obj("success" -> false, "err" -> "Incorrect message number")
      )

      val response6 = requests.post(host, data = ujson.Obj("name" -> "123123", "msg" -> "Test Message!", "replyTo" -> "asdas"))
      assert(
        ujson.read(response6) ==
          ujson.Obj("success" -> false, "err" -> "Incorrect message number")
      )
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }
  }
}