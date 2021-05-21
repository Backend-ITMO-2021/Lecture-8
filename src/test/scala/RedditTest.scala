import TestUtils.withServer
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

      assert(success.text().contains("Reddit: Thresh is mad :("))
      assert(success.text().contains("ventus976"))
      assert(
        success
          .text()
          .contains(
            "I don't particularly care which interaction they pick so long as it's consistent."
          )
      )
      assert(success.text().contains("XimbalaHu3"))
      assert(success.text().contains("Exactly, both is fine but do pick one."))
      assert(success.statusCode == 200)

      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("ventus976"))
      assert(
        wsMsg.contains(
          "I don't particularly care which interaction they pick so long as it's consistent."
        )
      )
      assert(wsMsg.contains("XimbalaHu3"))
      assert(wsMsg.contains("Exactly, both is fine but do pick one."))

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(
        host,
        data = ujson.Obj("username" -> "ilya", "msg" -> "Test Message!")
      )

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg2.contains("ventus976"))
      assert(
        wsMsg2.contains(
          "I don't particularly care which interaction they pick so long as it's consistent."
        )
      )
      assert(wsMsg2.contains("XimbalaHu3"))
      assert(wsMsg2.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg2.contains("ilya"))
      assert(wsMsg2.contains("Test Message!"))

      val success2 = requests.get(host)
      assert(success2.text().contains("ventus976"))
      assert(
        success2
          .text()
          .contains(
            "I don't particularly care which interaction they pick so long as it's consistent."
          )
      )
      assert(success2.text().contains("XimbalaHu3"))
      assert(success2.text().contains("Exactly, both is fine but do pick one."))
      assert(success2.text().contains("ilya"))
      assert(success2.text().contains("Test Message!"))
      assert(success2.statusCode == 200)
      assert(success2.text().contains("#1"))
      assert(success2.text().contains("#2"))
      assert(success2.text().contains("#3"))

      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(cask.Ws.Text("filter?=thresh"))

      val wsMsg3 = Await.result(wsPromise.future, Inf)
      assert(wsMsg3.contains("Test Message!"))
      assert(wsMsg3.contains("You can always trust Braum!"))

      val api1 = requests.get(s"$host/messages")
      val apiMsg1 = ujson.read(api1)
      assert(apiMsg1("messages").arr.length == 7)

      val api2 = requests.get(s"$host/messages/thresh")
      val apiMsg2 = ujson.read(api2)
      assert(apiMsg2("messages").arr.length == 3)

      wsPromise = scala.concurrent.Promise[String]
      val api3 = requests.post(
        s"$host/messages",
        data = ujson.Obj("username" -> "thresh", "message" -> "Nobody escapes.")
      )
      val apiMsg3 = ujson.read(api3)
      assert(apiMsg3("success") == ujson.True)

      val wsMsg4 = Await.result(wsPromise.future, Inf)
      assert(wsMsg4.contains("Nobody escapes."))
    }
    test("failure") - withServer(RedditApplication) { host =>
      val response1 =
        requests.post(
          host,
          data = ujson.Obj("username" -> "ilya"),
          check = false
        )
      assert(response1.statusCode == 400)
      val response2 =
        requests.post(host, data = ujson.Obj("username" -> "ilya", "msg" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(
        host,
        data = ujson.Obj("username" -> "", "msg" -> "Test Message!")
      )
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(
        host,
        data = ujson.Obj("username" -> "123#123", "msg" -> "Test Message!")
      )
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )
      val response5 = requests.post(
        s"$host/messages",
        data = ujson.Obj(
          "username" -> "thresh",
          "message" -> "Test Message!",
          "replyTo" -> 100
        )
      )
      assert(
        ujson.read(response5) ==
          ujson.Obj("success" -> false, "err" -> "Reply message doesn't exist")
      )
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }
  }
}
