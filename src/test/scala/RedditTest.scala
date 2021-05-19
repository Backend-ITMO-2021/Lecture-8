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
      val response = requests.post(host, data = ujson.Obj("username" -> "ilya", "msg" -> "Test Message!"))

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

      //Auto ID check
      assert(success2.text().contains("#1"))
      assert(success2.text().contains("#2"))
      assert(success2.text().contains("#3"))

      //Response test
      wsPromise = scala.concurrent.Promise[String]
      val response3 = requests.post(host, data = ujson.Obj("username" -> "ilya", "msg" -> "Test response message", "idParent" -> "1"))

      val success3 = requests.get(host)
      assert(success3.text().contains("#4"))
      assert(success3.text().indexOf("#4") < success3.text().indexOf("#2"))

      //Filter test
      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(cask.Ws.Text("ilya"))

      val wsMsg3 = Await.result(wsPromise.future, Inf)
      assert(!wsMsg3.contains("sergey"))
      assert(!wsMsg3.contains("XimbalaHu3"))
      assert(wsMsg3.contains("ilya"))
      assert(wsMsg3.contains("#3"))
      assert(wsMsg3.contains("#4"))

      //API test
      val apiResponse1 = requests.get(s"$host/messages")
      val apiParsed1 = ujson.read(apiResponse1)
      assert(apiParsed1("messages").arr.length == 4)

      val apiResponse2 = requests.get(s"$host/messages/ilya")
      val apiParsed2 = ujson.read(apiResponse2)
      assert(apiParsed2("messages").arr.length == 2)

      wsPromise = scala.concurrent.Promise[String]
      val apiResponse3 = requests.post(s"$host/messages", data = ujson.Obj("username" -> "sergey", "msg" -> "POST API Test"))
      val apiParsed3 = ujson.read(apiResponse3)
      assert(apiParsed3("success") == ujson.True)
      assert(apiParsed3("err") == ujson.Str(""))

      val wsMsg4 = Await.result(wsPromise.future, Inf)
      assert(wsMsg4.contains("POST API Test"))
    }
      

    test("failure") - withServer(RedditApplication) { host =>
      val response1 = requests.post(host, data = ujson.Obj("username" -> "ilya"), check = false)
      assert(response1.statusCode == 400)
      val response2 = requests.post(host, data = ujson.Obj("username" -> "ilya", "msg" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(host, data = ujson.Obj("username" -> "", "msg" -> "Test Message!"))
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(host, data = ujson.Obj("username" -> "123#123", "msg" -> "Test Message!"))
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )

      //Response check
      val response5 = requests.post(host, data = ujson.Obj("username" -> "sergey", "msg" -> "Test message", "idParent" -> "99"))
      assert(ujson.read(response5) == ujson.Obj("success" -> false, "err" -> "There is no message to reply"))
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }
  }
}