import TestUtils.withServer
import ru.ifmo.backend_2021.RedditApplication
import utest.{TestSuite, Tests, test}
import castor.Context.Simple.global
import cask.util.Logger.Console._
import cask.Ws
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
      val response = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!", "replyTo" -> ""))

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
    }
    test("failure") - withServer(RedditApplication) { host =>
      val response1 = requests.post(host, data = ujson.Obj("name" -> "ilya"), check = false)
      assert(response1.statusCode == 400)
      val response2 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "", "replyTo" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(host, data = ujson.Obj("name" -> "", "msg" -> "Test Message!", "replyTo" -> ""))
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(host, data = ujson.Obj("name" -> "123#123", "msg" -> "Test Message!", "replyTo" -> ""))
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }

    test("filter") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }
      val success = requests.get(host)
      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(Ws.Text("ventus976"))
      val filter = Await.result(wsPromise.future, Inf)

      assert(filter.contains("ventus976"))
      assert(filter.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(!filter.contains("XimbalaHu3"))
      assert(!filter.contains("Exactly, both is fine but do pick one."))
    }

    test("api_test") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]

      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }

      val getMessages = requests.get(host + "/messages")

      assert(getMessages.text().contains("ventus976"))
      assert(getMessages.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(getMessages.text().contains("XimbalaHu3"))
      assert(getMessages.text().contains("Exactly, both is fine but do pick one."))
      assert(getMessages.statusCode == 200)

      val getCurMessages = requests.get(host + "/messages/ventus976")
      assert(getCurMessages.text().contains("ventus976"))
      assert(getCurMessages.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(getCurMessages.statusCode == 200)

      val VladResponse = requests.post(host, data = ujson.Obj("name" -> "Vlad", "msg" -> "Hi, everyone!", "replyTo" -> ""))
      assert(
        ujson.read(VladResponse) ==
          ujson.Obj("success" -> true, "err" -> "")
      )
      val VladReplyResponse = requests.post(host, data = ujson.Obj("name" -> "Vlad", "msg" -> "Nice", "replyTo" -> "1"))
      assert(
        ujson.read(VladReplyResponse) ==
          ujson.Obj("success" -> true, "err" -> "")
      )
      val VladBadResponse = requests.post(host, data = ujson.Obj("name" -> "Vlad", "msg" -> "", "replyTo" -> ""))
      assert(
        ujson.read(VladBadResponse) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
    }

  }
}