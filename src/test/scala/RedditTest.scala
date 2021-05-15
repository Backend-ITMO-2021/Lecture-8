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

      assert(success.text().contains("Scala Reddit"))
      assert(success.text().contains("max"))
      assert(success.text().contains("Hi guys!"))
      assert(success.text().contains("anny"))
      assert(success.text().contains("Heeeyyyy"))
      assert(success.text().contains("anny"))
      assert(success.text().contains("So, what do you think guys about new Apple M2 chip?"))
      assert(success.text().contains("max"))
      assert(success.text().contains("I think they will not release it until 2022, M1 still very efficient"))
      assert(success.statusCode == 200)

      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("max"))
      assert(wsMsg.contains("Hi guys!"))
      assert(wsMsg.contains("anny"))
      assert(wsMsg.contains("Heeeyyyy"))
      assert(wsMsg.contains("anny"))
      assert(wsMsg.contains("So, what do you think guys about new Apple M2 chip?"))
      assert(wsMsg.contains("max"))
      assert(wsMsg.contains("I think they will not release it until 2022, M1 still very efficient"))

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("username" -> "ilya", "message" -> "Test Message!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg2.contains("max"))
      assert(wsMsg2.contains("Hi guys!"))
      assert(wsMsg2.contains("anny"))
      assert(wsMsg2.contains("Heeeyyyy"))
      assert(wsMsg2.contains("anny"))
      assert(wsMsg2.contains("So, what do you think guys about new Apple M2 chip?"))
      assert(wsMsg2.contains("max"))
      assert(wsMsg2.contains("I think they will not release it until 2022, M1 still very efficient"))
      assert(wsMsg2.contains("ilya"))
      assert(wsMsg2.contains("Test Message!"))

      val success2 = requests.get(host)
      assert(success2.text().contains("max"))
      assert(success2.text().contains("Hi guys!"))
      assert(success2.text().contains("anny"))
      assert(success2.text().contains("Heeeyyyy"))
      assert(success2.text().contains("anny"))
      assert(success2.text().contains("So, what do you think guys about new Apple M2 chip?"))
      assert(success2.text().contains("max"))
      assert(success2.text().contains("I think they will not release it until 2022, M1 still very efficient"))
      assert(success2.text().contains("ilya"))
      assert(success2.text().contains("Test Message!"))
      assert(success2.statusCode == 200)


      // Nesting and Auto Id's
      assert(success2.text().contains("#1"))
      assert(success2.text().contains("#2"))
      assert(success2.text().contains("#3"))
      assert(success2.text().contains("#4"))
      assert(success2.text().contains("#5"))

      wsPromise = scala.concurrent.Promise[String]
      val response3 = requests.post(host, data = ujson.Obj("username" -> "test", "message" -> "Test Message!", "replyTo" -> "1"))

      val success3 = requests.get(host)
      assert(success3.text().contains("#6"))
      assert(success3.text().indexOf("#6") < success3.text().indexOf("#5"))

      val wsMsg3 = Await.result(wsPromise.future, Inf)
      assert(wsMsg3.contains("#6"))
      assert(wsMsg3.indexOf("#6") < success3.text().indexOf("#5"))


      // Messages Filtering
      wsPromise = scala.concurrent.Promise[String]
      val response4 = requests.post(s"$host/filter", data = ujson.Obj("username" -> "max"))

      val wsMsg4 = Await.result(wsPromise.future, Inf)
      assert(wsMsg4.contains("max"))
      assert(wsMsg4.contains("#1"))
      assert(wsMsg4.contains("#4"))
      assert(!wsMsg4.contains("anny"))


      // Test API

      val api1 = requests.get(s"$host/messages")
      val api1Parsed = ujson.read(api1)
      assert(api1Parsed("messages").arr.length == 6)

      val api2 = requests.get(s"$host/messages/max")
      val api2Parsed = ujson.read(api2)
      assert(api2Parsed("messages").arr.length == 2)

      wsPromise = scala.concurrent.Promise[String]
      val api3 = requests.post(s"$host/messages", data = ujson.Obj("username" -> "usr", "message" -> "Test msg from API"))
      val api3Parsed = ujson.read(api3)
      assert(api3Parsed("success") == ujson.True)
      assert(api3Parsed("err") == ujson.Str(""))

      val wsMsg5 = Await.result(wsPromise.future, Inf)
      assert(wsMsg5.contains("Test msg from API"))
    }

    test("failure") - withServer(RedditApplication) { host =>
      val response1 = requests.post(host, data = ujson.Obj("username" -> "ilya"), check = false)
      assert(response1.statusCode == 400)
      val response2 = requests.post(host, data = ujson.Obj("username" -> "ilya", "message" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(host, data = ujson.Obj("username" -> "", "message" -> "Test Message!"))
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(host, data = ujson.Obj("username" -> "123#123", "message" -> "Test Message!"))
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }
  }
}