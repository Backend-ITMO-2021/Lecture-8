package ru.ifmo.backend_2021

import cask.WsChannelActor
import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.pseudodb.{MessageDB, PseudoDB}
import scalatags.Text.all._
import scalatags.generic
import scalatags.text.Builder

import scala.collection.mutable

object RedditApplication extends cask.MainRoutes {

  val serverUrl = s"http://$host:$port"

  val db: MessageDB = PseudoDB(s"db.txt", clean = true)

  val connectionPool: ConnectionPool = WsConnectionPool()

  val userFilters: mutable.HashMap[WsChannelActor, String] = mutable.HashMap[WsChannelActor, String]()

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
            input(`type` := "text", id := "replyToInput", placeholder := "Reply To (Optional)"),
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
            input(`type` := "submit", value := "Send"),
          ),
          form(onsubmit := "return filterMessages()")(
            input(`type` := "text", id := "filterInput", placeholder := "Filter Messages"),
            input(`type` := "submit", value := "Filter"),
          ),
        )
      )
    )
  )

  def messageList(conn: Option[WsChannelActor] = None): generic.Frag[Builder, String] = {
    val dbMessages = db.getMessages

    conn.flatMap(userFilters.get)
      .map(f => dbMessages.filter(_.username == f))
      .map(m => frag(m.map(renderMessage(0))))
      .getOrElse(printMessages(dbMessages))
  }

  private def printMessages(messages: List[Message]): generic.Frag[Builder, String] = frag(
    messages.filter(m => m.replyTo.isEmpty)
      .flatMap(sortMessages(messages))
      .map(printMessage(messages))
  )

  private def sortMessages(messages: List[Message])(message: Message): List[Message] =
    List(message).appendedAll(messages
      .filter(m => m.replyTo.contains(message.id.get))
      .flatMap(sortMessages(messages)))

  private def printMessage(messages: List[Message])(message: Message): generic.Frag[Builder, String] =
    renderMessage(LazyList.iterate(message.replyTo)(_.flatMap(p =>
      messages.filter(_.id.nonEmpty).find(_.id.get == p).flatMap(_.replyTo)
    )).takeWhile(_.nonEmpty).length)(message)

  private def renderMessage(offset: Int)(message: Message): generic.Frag[Builder, String] =
    frag(p(raw("&nbsp;&nbsp;&nbsp;&nbsp;" * offset), "#", message.id, " ", b(message.username), " ", message.message))

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool
    .wsHandler(connectionPool.send(Ws.Text(messageList().render))(_))(conn =>  {
      case Ws.Text(value) =>
        log.debug(conn, value)

        if (value.isBlank) userFilters.remove(conn)
        else userFilters(conn) = value

        connectionPool.send(Ws.Text(messageList(Some(conn)).render))(conn)

      case Ws.Close(_, _) =>
        log.debug("Close connection", conn)
        userFilters.remove(conn)
    })

  private def doPostChatMsg(name: String, msg: String, replyTo: Option[String]): Either[String, Message] = {
    log.debug(name, msg, replyTo)

    if (name.isBlank) Left("Name cannot be blank")
    else if (msg.isBlank) Left("Message cannot be blank")
    else if (name.contains("#")) Left("Username cannot contain '#'")
    else if (msg.contains("#")) Left("Message cannot contain '#'")
    else if (replyTo.nonEmpty && replyTo.get.toIntOption.isEmpty) Left("Reply To must be a number")
    else synchronized {
      db.addMessage(Message(None, name, msg, replyTo.flatMap(_.toIntOption))) match {
        case Left("replyTo") => Left("Reply To must mention another message")

        case Right(m) =>
          connectionPool.sendAll(conn => Ws.Text(messageList(Some(conn)).render))
          Right(m)
      }
    }
  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, replyTo: String = ""): ujson.Obj =
    doPostChatMsg(name, msg, Option(replyTo).filter(!_.isBlank)) match {
      case Left(e) => ujson.Obj("success" -> false, "err" -> e)
      case Right(_) => ujson.Obj("success" -> true, "err" -> "")
    }

  @cask.postJson("/messages")
  def postChatMsgApi(username: String, message: String, replyTo: ujson.Num = null): ujson.Obj =
    doPostChatMsg(username, message, Option(replyTo).map(_.value.toInt.toString)) match {
      case Left(e) => ujson.Obj("error" -> e)
      case Right(m) => msgToJson(m)
    }

  @cask.get("/messages/:username")
  def getUserChatMessages(username: String): ujson.Obj =
    ujson.Obj("messages" -> db.getMessages.filter(_.username == username).map(_.message))

  @cask.get("/messages")
  //noinspection AccessorLikeMethodIsEmptyParen
  def getChatMessages(): ujson.Obj = ujson.Obj(
    "messages" -> db.getMessages.map(msgToJson)
  )

  private def msgToJson(message: Message): ujson.Obj = ujson.Obj(
    "id" -> message.id.get,
    "username" -> message.username,
    "message" -> message.message,
    "replyTo" -> message.replyTo.map(n => ujson.Num(n)).getOrElse(ujson.Null),
  )

  log.debug(s"Starting at $serverUrl")
  initialize()
}
