package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.pseudodb.{MessageDB, PseudoDB}
import scalatags.Text.all._
import scalatags.generic
import scalatags.text.Builder
import ujson.Obj

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
            input(`type` := "text", id := "parentIdInput", placeholder := "Reply To (Optional)"),
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
            input(`type` := "submit", value := "Send")
          ),
          form(onsubmit := "return filter()")(
            input(`type` := "text", id := "filterInput", placeholder := "Filter Message"),
            input(`type` := "submit", value := "Send")
          )
        )
      )
    )
  )

  def filteredMessageList(name: String): generic.Frag[Builder, String] = frag {
    for {
      (lvl, msg) <- showCascade(db.getMessages).filter{ case (_, msg) => msg.username == name }
    } yield pre(" " * lvl, "#", msg.id, " ", b(msg.username), " ", msg.message)
  }

  def messageList(): generic.Frag[Builder, String] = frag {
    for {
      (lvl, msg) <- showCascade(db.getMessages)
    } yield pre(" " * lvl, "#", msg.id, " ", b(msg.username), " ", msg.message)
  }

  private def showCascade(messages: List[Message]): List[(Int, Message)] = {
    println(messages)
    val groupedMessages: Map[Int, List[Message]] = messages.groupMap(_.parentId)(identity).transform {
      case _ -> value => value.sortBy(_.id)
    }
    println(groupedMessages)

    def go(level: Int, messages: List[Message]): List[(Int, Message)] = {
      if (messages.isEmpty) {
        List.empty[(Int, Message)]
      } else {
        messages.flatMap(msg => (level, msg) +: go(level + 1, groupedMessages.getOrElse(msg.id, List.empty)))
      }
    }

    go(0, groupedMessages(0))
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, replyTo: Int = 0): ujson.Obj = {
    log.debug(name, msg)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else synchronized {
      db.addMessage(new Message(db.getMessages.maxByOption(_.id).map(_.id).getOrElse(0) + 1, replyTo, name, msg))
      connectionPool.sendAll(Ws.Text(messageList().render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.getJson("/filter")
  def filterMessages(name: String = ""): ujson.Obj = {
    if (name.isEmpty) {
      connectionPool.sendAll(Ws.Text(messageList().render))
    } else {
      connectionPool.sendAll(Ws.Text(filteredMessageList(name).render))
    }
    ujson.Obj("success" -> true, "err" -> "")
  }

  @cask.postJson("/messages")
  def addMessage(username: String, message: String, replyTo: Int = 0): Obj = postChatMsg(username, message, replyTo)

  @cask.get(s"/messages/:username")
  def specifiedUserMessages(username: String): Obj = {
    ujson.Obj("messages" -> db.getMessages.filter(_.username == username).map(_.message))
  }

  @cask.get("/messages")
  def allMessages(): Obj = {
    Obj("messages" -> db.getMessages.map {
      case Message(id, 0, name, text) => ujson.Obj("id" -> id, "username" -> name, "message" -> text)
      case Message(id, pId, name, text) => ujson.Obj("id" -> id, "username" -> name, "message" -> text, "replyTo" -> pId)
    })
  }

  log.debug(s"Starting at $serverUrl")
  initialize()
}
