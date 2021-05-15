package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.pseudodb.{MessageDB, PseudoDB}
import scalatags.Text.all._
import scalatags.Text.tags2
import scalatags.generic
import scalatags.text.Builder
import ApplicationUtils.messageList

case class AppStatic()(implicit cc: castor.Context, log: cask.Logger) extends cask.Routes {
  val db: MessageDB = ApplicationUtils.getDB
  val connectionPool: ConnectionPool = WsConnectionPool()

  @cask.staticResources("/static")
  def staticResourceRoutes() = "static"

  @cask.get("/")
  def hello(): Document = doctype("html")(
    html(
      head(
        tags2.title("Scala Reddit"),
        link(rel := "stylesheet", href := ApplicationUtils.styles),
        link(rel := "icon", `type` := "image/png", href := "/static/favicon.png"),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container border border-2 border-primary my-5 p-5", css("max-width") := "700px", css("border-radius") := "10px")(
          h1(cls := "mb-5")("Scala Reddit"),

          div(id := "messageList", cls := "mb-5")(messageList(db.getMessages)),

          div(cls := "mb-3 text-danger fw-bold", id := "errorDiv"),

          form(onsubmit := "return submitForm()")(
            div(cls := "input-group")(
              input(`type` := "text", id := "replyInput", cls := "form-control", placeholder := "Reply (Optional)"),
              input(`type` := "text", id := "nameInput", cls := "form-control", placeholder := "Username"),
              input(`type` := "text", id := "msgInput", cls := "form-control", placeholder := "Write a message!"),
              button(`type` := "submit", cls := "btn btn-primary", "Send")
            )
          ),

          form(onsubmit := "return submitFilter()")(
            div(cls := "input-group mt-3")(
              input(`type` := "text", id := "filterInput", cls := "form-control", placeholder := "Filter By User"),
              button(`type` := "submit", cls := "btn btn-secondary", "Filter")
            )
          )
        )
      )
    )
  )

  @cask.postJson("/")
  def postMsg(username: String, message: String, replyTo: String = ""): ujson.Obj = {
    log.debug("WebView: / (POST) ", username, message, replyTo)

    if (username == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (message == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (replyTo.contains("#")) ujson.Obj("success" -> false, "err" -> "Reply To cannot contain '#'")
    else synchronized {
      db.appendMessage(username, message, Option.unless(replyTo == "")(replyTo.toInt))
      connectionPool.sendAll(Ws.Text(messageList(db.getMessages).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.postJson("/filter")
  def userMsg(username: String): ujson.Obj = {
    synchronized {
      connectionPool.sendAll(Ws.Text(messageList(db.getMessages, Option.unless(username.length == 0)(username)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList(db.getMessages).render))(connection)
  }

  initialize()
}