package ru.ifmo.backend_2021

import scala.collection.mutable.ListBuffer

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.pseudodb.{MessageDB, PseudoDB}
import scalatags.Text.all._
import scalatags.Text.tags2
import scalatags.generic
import scalatags.text.Builder

object RedditApplication extends cask.MainRoutes {
  val serverUrl = s"http://$host:$port"
  val db: MessageDB = PseudoDB(s"db.txt", clean = true)
  val connectionPool: ConnectionPool = WsConnectionPool()

  @cask.staticResources("/static")
  def staticResourceRoutes() = "static"

  @cask.get("/")
  def hello(): Document = doctype("html")(
    html(
      head(
        tags2.title("Scala Chat"),
        link(rel := "stylesheet", href := ApplicationUtils.styles),
        link(rel := "icon", `type` := "image/png", href := "/static/favicon.png"),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container border rounded my-5 p-5", css("max-width") := "700px")(
          h1(cls := "mb-5")("Reddit: Swain is mad :("),

          div(id := "messageList")(messageList()),

          div(cls := "mt-5", id := "errorDiv", color.red),
          form(onsubmit := "return submitForm()")(
            div(cls := "input-group")(
              input(`type` := "text", id := "replyInput", cls := "form-control", placeholder := "Reply (Optional)"),
              input(`type` := "text", id := "nameInput", cls := "form-control", placeholder := "Username"),
              input(`type` := "text", id := "msgInput", cls := "form-control", placeholder := "Write a message!"),
              button(`type` := "submit", cls := "btn btn-outline-primary", "Send")
            )
          )
        )
      )
    )
  )

  def messageList(): generic.Frag[Builder, String] = {
    def buildMessageThread(message: Message, depth: Int, lb: ListBuffer[Frag], groupedMessages: Map[Option[Int], List[Message]]): Unit = {
      val Message(id, name, msg, _) = message
      lb.append(
        p(span(cls := "text-secondary", css("white-space") := "pre-wrap", "    " * depth), span(s"#$id"), " ", b(name), " ", msg)
      )

      val children = groupedMessages.get(Option(message.id))
      if (children.isDefined) {
        children.get.foreach(childMessage => buildMessageThread(childMessage, depth + 1, lb, groupedMessages))
      }
    }

    val messages = db.getMessages
    val messagesGroupedByRoot = messages.groupBy(_.parentId)
    val lb = ListBuffer[Frag]()
    val rootMessages = messagesGroupedByRoot.get(None)

    rootMessages.get.foreach(rootMessage => {
      buildMessageThread(rootMessage, 0, lb, messagesGroupedByRoot)
    })

    lb.result()
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, reply: String): ujson.Obj = {
    log.debug(name, msg, reply)

    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (reply.contains("#")) ujson.Obj("success" -> false, "err" -> "Reply To cannot contain '#'")
    else synchronized {
      val nextID = db.getMessages.length + 1

      if (reply.length > 0) {
        db.addMessage(Message(nextID, name, msg, Some(reply.toInt)))
      } else {
        db.addMessage(Message(nextID, name, msg, None))
      }

      connectionPool.sendAll(Ws.Text(messageList().render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  log.debug(s"Starting at $serverUrl")
  initialize()
}
