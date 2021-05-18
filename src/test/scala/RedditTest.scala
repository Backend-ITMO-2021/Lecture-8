import TestUtils.{readJsonAndAssert, withServer}
import cask.Ws
import ru.ifmo.backend_2021.RedditApplication
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
      assert(success.text().contains("#1"))
      assert(success.text().contains("ventus976"))
      assert(success.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success.text().contains("#2"))
      assert(success.text().contains("XimbalaHu3"))
      assert(success.text().contains("Exactly, both is fine but do pick one."))
      assert(success.statusCode == 200)

      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("#1"))
      assert(wsMsg.contains("ventus976"))
      assert(wsMsg.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg.contains("#2"))
      assert(wsMsg.contains("XimbalaHu3"))
      assert(wsMsg.contains("Exactly, both is fine but do pick one."))

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg2.contains("#1"))
      assert(wsMsg2.contains("ventus976"))
      assert(wsMsg2.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg2.contains("#2"))
      assert(wsMsg2.contains("XimbalaHu3"))
      assert(wsMsg2.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg2.contains("#3"))
      assert(wsMsg2.contains("ilya"))
      assert(wsMsg2.contains("Test Message!"))

      val success2 = requests.get(host)
      assert(success2.text().contains("#1"))
      assert(success2.text().contains("ventus976"))
      assert(success2.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success2.text().contains("#2"))
      assert(success2.text().contains("XimbalaHu3"))
      assert(success2.text().contains("Exactly, both is fine but do pick one."))
      assert(success2.text().contains("#3"))
      assert(success2.text().contains("ilya"))
      assert(success2.text().contains("Test Message!"))
      assert(success2.statusCode == 200)

      readJsonAndAssert(requests.get(s"$host/messages")){ resp => List(
        resp("messages")(0)("id") == ujson.Num(1),
        resp("messages")(0)("username") == ujson.Str("ventus976"),
        resp("messages")(0)("message") == ujson.Str("I don't particularly care which interaction they pick so long as it's consistent."),
        resp("messages")(1)("id") == ujson.Num(2),
        resp("messages")(1)("username") == ujson.Str("XimbalaHu3"),
        resp("messages")(1)("message") == ujson.Str("Exactly, both is fine but do pick one."),
        resp("messages")(2)("id") == ujson.Num(3),
        resp("messages")(2)("username") == ujson.Str("ilya"),
        resp("messages")(2)("message") == ujson.Str("Test Message!"),
      ).forall(identity) }

      wsPromise = scala.concurrent.Promise[String]
      val response2 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message 1!", "replyTo" -> "3"))

      val parsed2 = ujson.read(response2)
      assert(parsed2("success") == ujson.True)
      assert(parsed2("err") == ujson.Str(""))

      assert(response2.statusCode == 200)
      val wsMsg3 = Await.result(wsPromise.future, Inf)
      assert(wsMsg3.contains("#1"))
      assert(wsMsg3.contains("ventus976"))
      assert(wsMsg3.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg3.contains("#2"))
      assert(wsMsg3.contains("XimbalaHu3"))
      assert(wsMsg3.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg3.contains("#3"))
      assert(wsMsg3.contains("ilya"))
      assert(wsMsg3.contains("Test Message!"))
      assert(wsMsg3.contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(wsMsg3.contains("ilya"))
      assert(wsMsg3.contains("Test Message 1!"))

      val success3 = requests.get(host)
      assert(success3.text().contains("#1"))
      assert(success3.text().contains("ventus976"))
      assert(success3.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success3.text().contains("#2"))
      assert(success3.text().contains("XimbalaHu3"))
      assert(success3.text().contains("Exactly, both is fine but do pick one."))
      assert(success3.text().contains("#3"))
      assert(success3.text().contains("ilya"))
      assert(success3.text().contains("Test Message!"))
      assert(success3.text().contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(success3.text().contains("ilya"))
      assert(success3.text().contains("Test Message 1!"))
      assert(success3.statusCode == 200)

      readJsonAndAssert(requests.get(s"$host/messages")){ resp => List(
        resp("messages")(0)("id") == ujson.Num(1),
        resp("messages")(0)("username") == ujson.Str("ventus976"),
        resp("messages")(0)("message") == ujson.Str("I don't particularly care which interaction they pick so long as it's consistent."),
        resp("messages")(1)("id") == ujson.Num(2),
        resp("messages")(1)("username") == ujson.Str("XimbalaHu3"),
        resp("messages")(1)("message") == ujson.Str("Exactly, both is fine but do pick one."),
        resp("messages")(2)("id") == ujson.Num(3),
        resp("messages")(2)("username") == ujson.Str("ilya"),
        resp("messages")(2)("message") == ujson.Str("Test Message!"),
        resp("messages")(3)("id") == ujson.Num(4),
        resp("messages")(3)("username") == ujson.Str("ilya"),
        resp("messages")(3)("message") == ujson.Str("Test Message 1!"),
        resp("messages")(3)("replyTo") == ujson.Num(3),
      ).forall(identity) }

      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(Ws.Text("ilya"))

      val wsMsg4 = Await.result(wsPromise.future, Inf)
      assert(!wsMsg4.contains("#1"))
      assert(!wsMsg4.contains("ventus976"))
      assert(!wsMsg4.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(!wsMsg4.contains("#2"))
      assert(!wsMsg4.contains("XimbalaHu3"))
      assert(!wsMsg4.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg4.contains("#3"))
      assert(wsMsg4.contains("ilya"))
      assert(wsMsg4.contains("Test Message!"))
      assert(!wsMsg4.contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(wsMsg4.contains("#4"))
      assert(wsMsg4.contains("ilya"))
      assert(wsMsg4.contains("Test Message 1!"))

      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(Ws.Text("ventus976"))

      val wsMsg5 = Await.result(wsPromise.future, Inf)
      assert(wsMsg5.contains("#1"))
      assert(wsMsg5.contains("ventus976"))
      assert(wsMsg5.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(!wsMsg5.contains("#2"))
      assert(!wsMsg5.contains("XimbalaHu3"))
      assert(!wsMsg5.contains("Exactly, both is fine but do pick one."))
      assert(!wsMsg5.contains("#3"))
      assert(!wsMsg5.contains("ilya"))
      assert(!wsMsg5.contains("Test Message!"))
      assert(!wsMsg5.contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(!wsMsg5.contains("ilya"))
      assert(!wsMsg5.contains("Test Message 1!"))

      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(Ws.Text("    "))

      val wsMsg6 = Await.result(wsPromise.future, Inf)
      assert(wsMsg6.contains("#1"))
      assert(wsMsg6.contains("ventus976"))
      assert(wsMsg6.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg6.contains("#2"))
      assert(wsMsg6.contains("XimbalaHu3"))
      assert(wsMsg6.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg6.contains("#3"))
      assert(wsMsg6.contains("ilya"))
      assert(wsMsg6.contains("Test Message!"))
      assert(wsMsg6.contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(wsMsg6.contains("ilya"))
      assert(wsMsg6.contains("Test Message 1!"))

      wsPromise = scala.concurrent.Promise[String]
      val response3 = requests.post(s"$host/messages", data = ujson.Obj("username" -> "oleg", "message" -> "Test Message 2!"))

      val parsed3 = ujson.read(response3)
      assert(parsed3("id") == ujson.Num(5))
      assert(parsed3("username") == ujson.Str("oleg"))
      assert(parsed3("message") == ujson.Str("Test Message 2!"))

      val success4 = requests.get(host)
      assert(success4.text().contains("#1"))
      assert(success4.text().contains("ventus976"))
      assert(success4.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success4.text().contains("#2"))
      assert(success4.text().contains("XimbalaHu3"))
      assert(success4.text().contains("Exactly, both is fine but do pick one."))
      assert(success4.text().contains("#3"))
      assert(success4.text().contains("ilya"))
      assert(success4.text().contains("Test Message!"))
      assert(success4.text().contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(success4.text().contains("ilya"))
      assert(success4.text().contains("Test Message 1!"))
      assert(success4.text().contains("#5"))
      assert(success4.text().contains("oleg"))
      assert(success4.text().contains("Test Message 2!"))
      assert(success4.statusCode == 200)

      val wsMsg7 = Await.result(wsPromise.future, Inf)
      assert(wsMsg7.contains("#1"))
      assert(wsMsg7.contains("ventus976"))
      assert(wsMsg7.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg7.contains("#2"))
      assert(wsMsg7.contains("XimbalaHu3"))
      assert(wsMsg7.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg7.contains("#3"))
      assert(wsMsg7.contains("ilya"))
      assert(wsMsg7.contains("Test Message!"))
      assert(wsMsg7.contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(wsMsg7.contains("ilya"))
      assert(wsMsg7.contains("Test Message 1!"))
      assert(wsMsg7.contains("#5"))
      assert(wsMsg7.contains("oleg"))
      assert(wsMsg7.contains("Test Message 2!"))

      wsPromise = scala.concurrent.Promise[String]
      val response4 = requests.post(s"$host/messages", data = ujson.Obj("username" -> "oleg", "message" -> "Test Message 3!", "replyTo" -> 4))

      val parsed4 = ujson.read(response4)
      assert(parsed4("id") == ujson.Num(6))
      assert(parsed4("username") == ujson.Str("oleg"))
      assert(parsed4("message") == ujson.Str("Test Message 3!"))

      val success5 = requests.get(host)
      assert(success5.text().contains("#1"))
      assert(success5.text().contains("ventus976"))
      assert(success5.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success5.text().contains("#2"))
      assert(success5.text().contains("XimbalaHu3"))
      assert(success5.text().contains("Exactly, both is fine but do pick one."))
      assert(success5.text().contains("#3"))
      assert(success5.text().contains("ilya"))
      assert(success5.text().contains("Test Message!"))
      assert(success5.text().contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(success5.text().contains("ilya"))
      assert(success5.text().contains("Test Message 1!"))
      assert(success5.text().contains("#5"))
      assert(success5.text().contains("oleg"))
      assert(success5.text().contains("Test Message 2!"))
      assert(success5.statusCode == 200)

      val success6 = requests.get(host)
      assert(success6.text().contains("#1"))
      assert(success6.text().contains("ventus976"))
      assert(success6.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success6.text().contains("#2"))
      assert(success6.text().contains("XimbalaHu3"))
      assert(success6.text().contains("Exactly, both is fine but do pick one."))
      assert(success6.text().contains("#3"))
      assert(success6.text().contains("ilya"))
      assert(success6.text().contains("Test Message!"))
      assert(success6.text().contains("&nbsp;&nbsp;&nbsp;&nbsp;#4"))
      assert(success6.text().contains("ilya"))
      assert(success6.text().contains("Test Message 1!"))
      assert(success6.text().contains("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;#6"))
      assert(success6.text().contains("ilya"))
      assert(success6.text().contains("Test Message 3!"))
      assert(success6.text().contains("#5"))
      assert(success6.text().contains("oleg"))
      assert(success6.text().contains("Test Message 2!"))
      assert(success6.statusCode == 200)

      readJsonAndAssert(requests.get(s"$host/messages")){ resp => List(
        resp("messages")(0)("id") == ujson.Num(1),
        resp("messages")(0)("username") == ujson.Str("ventus976"),
        resp("messages")(0)("message") == ujson.Str("I don't particularly care which interaction they pick so long as it's consistent."),
        resp("messages")(1)("id") == ujson.Num(2),
        resp("messages")(1)("username") == ujson.Str("XimbalaHu3"),
        resp("messages")(1)("message") == ujson.Str("Exactly, both is fine but do pick one."),
        resp("messages")(2)("id") == ujson.Num(3),
        resp("messages")(2)("username") == ujson.Str("ilya"),
        resp("messages")(2)("message") == ujson.Str("Test Message!"),
        resp("messages")(3)("id") == ujson.Num(4),
        resp("messages")(3)("username") == ujson.Str("ilya"),
        resp("messages")(3)("message") == ujson.Str("Test Message 1!"),
        resp("messages")(3)("replyTo") == ujson.Num(3),
        resp("messages")(4)("id") == ujson.Num(5),
        resp("messages")(4)("username") == ujson.Str("oleg"),
        resp("messages")(4)("message") == ujson.Str("Test Message 2!"),
        resp("messages")(5)("id") == ujson.Num(6),
        resp("messages")(5)("username") == ujson.Str("oleg"),
        resp("messages")(5)("message") == ujson.Str("Test Message 3!"),
        resp("messages")(5)("replyTo") == ujson.Num(4),
      ).forall(identity) }

      readJsonAndAssert(requests.get(s"$host/messages/ilya"))(_ == ujson.Obj(
        "messages" -> ujson.Arr(
          ujson.Str("Test Message!"),
          ujson.Str("Test Message 1!"),
        )
      ))

      readJsonAndAssert(requests.get(s"$host/messages/oleg"))(_ == ujson.Obj(
        "messages" -> ujson.Arr(
          ujson.Str("Test Message 2!"),
          ujson.Str("Test Message 3!"),
        )
      ))
    }

    test("failure") - withServer(RedditApplication) { host =>
      assert(requests.post(host, data = ujson.Obj("name" -> "ilya"), check = false).statusCode == 400)

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> ""))
      )(_ == ujson.Obj("success" -> false, "err" -> "Message cannot be blank"))

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "   "))
      )(_ == ujson.Obj("success" -> false, "err" -> "Message cannot be blank"))

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "", "msg" -> "Test Message!"))
      )(_ == ujson.Obj("success" -> false, "err" -> "Name cannot be blank"))

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "   ", "msg" -> "Test Message!"))
      )(_ == ujson.Obj("success" -> false, "err" -> "Name cannot be blank"))

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "123#123", "msg" -> "Test Message!"))
      )(_ == ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'"))

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test#Message!"))
      )(_ == ujson.Obj("success" -> false, "err" -> "Message cannot contain '#'"))

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!", "replyTo" -> "test"))
      )(_ == ujson.Obj("success" -> false, "err" -> "Reply To must be a number"))

      readJsonAndAssert(
        requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!", "replyTo" -> "123"))
      )(_ == ujson.Obj("success" -> false, "err" -> "Reply To must mention another message"))
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }
  }
}
