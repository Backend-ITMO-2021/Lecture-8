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
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
            input(`type` := "text", id := "replyToInput", placeholder := "Reply to?"),
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

  def messageList(DBbuf: List[Message] = db.getMessages.filter(_.replyTo.getOrElse(0) == 0), lvl: Int = 0): generic.Frag[Builder, String] = {
    if (DBbuf.isEmpty) return frag()
    for (message <- DBbuf)
      yield frag(p("#", message.id, " ", b(message.username), " ", message.message, marginLeft.:=(lvl*20)), messageList(db.getMessages.filter(_.replyTo.getOrElse(0) == message.id), lvl+1))
  }

  def filterMessageList(filter: Option[String], DBbuf: List[Message] = db.getMessages, lvl: Int = 0): generic.Frag[Builder, String] = {
    if (filter.isEmpty){
      return messageList()
    }
    if (!DBbuf.exists(_.username == filter.get)) return frag()
    for (message <- DBbuf.filter(_.username == filter.get))
      yield frag(p("#", message.id, " ", b(message.username), " ", message.message, marginLeft.:=(lvl*20)))
  }


  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }


  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, replyTo: String = ""): ujson.Obj = {
    log.debug(name, msg, replyTo)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else synchronized {
      db.addNewMessage(name, msg, replyTo.toIntOption)
      connectionPool.sendAll(Ws.Text(messageList().render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/messages")
  def getAllMessages(): ujson.Obj = ujson.Obj(
    "messages" -> messagesToJson(db.getMessages)
  )

  @cask.get("/messages/:user")
  def getAllUserMessages(user: String): ujson.Obj = ujson.Obj(
    "messages" -> messagesToJson(db.getMessages.filter(_.username == user))
  )

  @cask.postJson("/messages")
  def postMsg(name: String = "", msg: String = "", replyTo: String = ""): ujson.Obj = postChatMsg(name, msg, replyTo)


  def messagesToJson(messages: List[Message]): List[ujson.Obj] = {
    for (Message(id, username, msg, replyTo) <- messages) yield messageToJson(id, username, msg, replyTo.getOrElse("").toString)
  }

  def messageToJson(id: Int, username: String, msg: String, replyTo: String): ujson.Obj = ujson.Obj(
    "id" -> id,
    "username" -> username,
    "message" -> msg,
    "replyTo" -> replyTo
  )



  log.debug(s"Starting at $serverUrl")
  initialize()
}
