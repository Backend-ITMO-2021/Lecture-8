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
        link(rel := "stylesheet", href := ApplicationUtils.styles2),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container")(
          h1("Reddit: Swain is mad :("),
          div(id := "messageList")(messageList()),
          div(id := "errorDiv", color.red),
          form(onsubmit := "return submitForm()")(
            input(`type` := "text", id := "replyIDInput", placeholder := "Reply To (Optional)"),
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
            input(`type` := "submit", value := "Send")
          ),
          form(onsubmit := "return submitFilter()")(
            input(`type` := "text", id := "filterInput", placeholder := "Filter Messages"),
            input(`type` := "submit", value := "Filter"),
          )
        )
      )
    )
  )

  def messageList(nameFilter: Option[String] = None): generic.Frag[Builder, String] = {
    val messagesGroupedByParent = db.getMessages.groupBy(_.idParent)

    def getChildren(groupedMessages: Map[String, List[Message]], depth: Int = 0, idParent: String = "none"): Frag = {
      val oneParentMsg = groupedMessages.get(idParent)
      if (oneParentMsg.isEmpty) return frag()
      for (message <- oneParentMsg.get) yield frag(renderMessage(message, depth), getChildren(groupedMessages, depth + 1, message.id))
    }

    def renderMessage(message: Message, tabs: Int = 0): generic.Frag[Builder, String] = {
      val Message(id, name, msg, _) = message
      p("\t"*tabs, "#", id, " ", b(name), " ", msg)
    }
    if (nameFilter.isDefined) {
      val filteredMsg = db.getMessages.filter(_.username == nameFilter.get)
      frag(for (msg <- filteredMsg) yield renderMessage(msg))
    } else getChildren(messagesGroupedByParent)
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(username: String, msg: String, idParent: String = "none"): ujson.Obj = {
    log.debug(username, msg)
    var idP = idParent
    if(idParent == "") {idP = "none"}
    if (username == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (idP != "none" && !db.getMessages.exists(_.id == idP)) ujson.Obj("success" -> false, "err" -> "There is no message to reply")
    else synchronized {
      val id = (db.getMessages.length + 1).toString()
      db.addMessage(Message(id, username, msg, idP))
      connectionPool.sendAll(Ws.Text(messageList().render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.postJson("/messages")
  def postMessage(username: String, msg: String, idParent: String = "none"): ujson.Obj = {
    if (username == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (idParent != "none" && !db.getMessages.exists(_.id == idParent)) ujson.Obj("success" -> false, "err" -> "There is no message to reply")
    else {
      val messageId = (db.getMessages.length + 1).toString()
      db.addMessage(
        Message(messageId, username, msg, idParent)
      )
      connectionPool.sendAll(Ws.Text(messageList().render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/messages/:username")
  def getUserMessage(username: String): ujson.Obj =
    ujson.Obj(
      "username" -> username,
      "messages" -> db.getMessages.filter(_.username == username).map(_.message)
    )

  @cask.get("/messages")
  def getMessages(): ujson.Obj =
    ujson.Obj("messages" -> db.getMessages.map(message => {
      ujson.Obj(
        "id" -> message.id,
        "username" -> message.username,
        "message" -> message.message,
        "idParent" -> message.idParent
      )
    }))

  log.debug(s"Starting at $serverUrl")
  initialize()
}
