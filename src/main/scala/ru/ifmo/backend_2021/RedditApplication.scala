package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.pseudodb.{MessageDB, PseudoDB}
import scalatags.Text.all._
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
        link(rel := "stylesheet", href := ApplicationUtils.styles),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container")(
          h1("Reddit: Swain is mad :("),
          div(id := "messageList")(messageList()),
          div(id := "errorDiv", color.red),
          form(onsubmit := "return submitForm()")(
            input(`type` := "number", id := "replyInput", placeholder := "Reply To (Optional)"),
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
            input(`type` := "submit", value := "Send"),
          ),
          form(onsubmit := "return submitFilter()")(
            input(`type` := "text", id := "filterInput", placeholder := "Filter Messages"),
            input(`type` := "submit", value := "Submit"),
          )
        )
      )
    )
  )

  def messageList(filter: Option[String] = None): generic.Frag[Builder, String] = {
    def nextMessage(sortedMessages: Map[Option[Int], List[Message]], indents: Int, parent: Option[Int]): generic.Frag[Builder, String] = {
      val messages = sortedMessages.get(parent)
      if (messages.isDefined) {
        for (message <- messages.get) yield frag(
          p(css("white-space") := "pre", List.fill(indents)("    ").mkString + "#", message.id, " ", b(message.username), " ", message.message),
          nextMessage(sortedMessages, indents + 1, Some(message.id)))
      }
      else
        frag()

    }

    def filterMessage(messages: List[Message], name: String): generic.Frag[Builder, String] = {
      val filteredMessages = messages.filter(_.username == name)

      if (filteredMessages.nonEmpty)
        for (message <- filteredMessages)
          yield frag(p("#", message.id, " ", b(message.username), " ", message.message))
      else
        frag()

    }

    if (filter.isDefined & filter.getOrElse("") != "") {
      filterMessage(db.getMessages, filter.get.trim)
    } else {
      val sortedMessages = db.getMessages.groupBy(_.parent)
      nextMessage(sortedMessages, 0, None)
    }

  }



  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.sendAll(connection => Ws.Text(messageList().render))  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, parent: String =""): ujson.Obj = {
    log.debug(name, msg, parent)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (parent.contains("#")) ujson.Obj("success" -> false, "err" -> "Reply To cannot contain '#'")
    else synchronized {
      db.addMessage(Message(db.getMessages.length + 1, name, msg, parent.toIntOption))
      connectionPool.sendAll(connection => Ws.Text(messageList(connectionPool.getFilter(connection)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/messages")
  def getAllMessages(): ujson.Obj =
    ujson.Obj("messages" -> db.getMessages.map(message => {
      ujson.Obj(
        "id" -> message.id,
        "username" -> message.username,
        "message" -> message.message,
        "replyTo" -> message.parent
      )
    }))

  @cask.get("/messages/:username")
  def getUserMessages(username: String): ujson.Obj =
    ujson.Obj("messages" -> db.getMessages.filter(_.username == username.trim).map(message => {
      ujson.Obj("message" -> message.message)
    }))

  @cask.postJson("/messages")
  def postMessage(name: String, msg: String, parent: String =""): ujson.Obj = {
    log.debug(name, msg, parent)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (parent.contains("#")) ujson.Obj("success" -> false, "err" -> "Reply To cannot contain '#'")
    else synchronized {
      db.addMessage(Message(db.getMessages.length + 1, name, msg, parent.toIntOption))
      connectionPool.sendAll(connection => Ws.Text(messageList().render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }



  log.debug(s"Starting at $serverUrl")
  initialize()
}
