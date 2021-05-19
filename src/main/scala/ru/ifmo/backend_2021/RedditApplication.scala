package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.RedditApplication.{filter, isCascade}
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.pseudodb.{MessageDB, PseudoDB}
import scalatags.Text.all._
import scalatags.{Text, generic}
import scalatags.text.Builder
import ujson.Obj

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object RedditApplication extends cask.MainRoutes {
  val serverUrl = s"http://$host:$port"
  val db: MessageDB = PseudoDB(s"db.txt", clean = true)
  val connectionPool: ConnectionPool = WsConnectionPool()
  var isCascade = false
  var filter: Option[String] = None

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
        div(cls := "container", display := "flex")(
          div(cls := "chat-container")(
            h1("Reddit: Swain is mad :("),
            div(style := "margin-top: 25px;")(
              form(onsubmit := "return filterForm()")(
                h4(b("User filter:")),
                input(`type` := "text", id := "filterInput", placeholder := "Enter user name"),
                input(`type` := "submit", value := "Apply"),
              )
            ),
            div(id := "messageList", style := "margin-top: 27px;")(messageList()),
            div(id := "errorDiv", color.red),
            form(onsubmit := "return submitForm()")(
              input(`type` := "text", id := "toInput", placeholder := "Reply to (can be empty)"),
              input(`type` := "text", id := "nameInput", placeholder := "Username"),
              input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
              input(`type` := "submit", value := "Send"),
            )
          ),
          div(cls := "aside")(
            div(id := "displayMode")(displayMode()),
            form(onsubmit := "return changeDisplayForm()")(
              input(`type` := "submit", id := "cascade", value := "Change display mode")
            )
          )
        )
      )
    )
  )

  def messageList(): generic.Frag[Builder, String] = {
    val messages = filter match {
      case Some(filterValue) => db.getMessages.filter(msg => msg.username.contains(filterValue))
      case None => db.getMessages
    }
    if (messages.isEmpty) {
      frag("No messages");
    } else {
      isCascade match {
        case false => frag(for (Message(id, to, name, msg) <- messages) yield p(i(s"#$id"), " ", if(to.isDefined) s"-> #${to.get}" else "  ", "   ",  b(name), " ", msg))
        case true =>
          val withoutTo = mutable.LinkedHashMap[String, Message]()
          val replies = mutable.HashMap[String, ListBuffer[Message]]()
          messages.foreach(message => {
            message.to match {
              case Some(to_id) => replies.get(to_id) match {
                case Some(l) => l.addOne(message)
                case None => replies.addOne(to_id, ListBuffer().addOne(message))
              }
              case None => withoutTo.addOne(message.id, message)
            }
          })

          val msgListBuilder = new ListBuffer[Text.TypedTag[String]]
          def addReply(msg_id: String, repl: mutable.HashMap[String, ListBuffer[Message]], sb: ListBuffer[Text.TypedTag[String]], k: Int = 0): Unit = {
            repl.get(msg_id) match {
              case Some(repList) => repList.foreach(reply => {
                sb.addOne(div(style := "margin-bottom: 18px")(for (_ <- 0 until k + 1) yield "-",  reply.toListItemStr))
                repl.get(reply.id) match {
                  case Some(_) => addReply(reply.id, repl, sb, k + 1)
                  case None => ()
                }
              })
              case None => ()
            }
          }
          withoutTo.foreach(withoutMsg => {
            msgListBuilder.addOne(div(style := "margin-bottom: 18px")(withoutMsg._2.toListItemStr))
            addReply(withoutMsg._1, replies, msgListBuilder)
          })
          frag(msgListBuilder.toList)
      }
    }
  }

  def displayMode(): generic.Frag[Builder, String] = {
    frag(h3(s"Display: ${if (isCascade) "Cascade" else "Column"}"))
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  def renderList(): ujson.Obj = {
    connectionPool.sendAll(Ws.Text(messageList().render))
    ujson.Obj("success" -> true, "err" -> "")
  }

  @cask.postJson("/")
  def postChatMsg(to: String, name: String, msg: String): ujson.Obj = {
    log.debug(name, msg)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#") || to.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (to == "_") ujson.Obj("success" -> false, "err" -> "Target user cannot be '_'")
    else synchronized {
      val lastId = db.getLastId
      db.addMessage(Message((lastId.toInt + 1).toString, if (to != "") Some(to) else None, name, msg)) //todo
      renderList()
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.postJson("/cascade")
  def cascadeToggle(): ujson.Obj = {
    println("Toggle cascade")
    isCascade = !isCascade
    connectionPool.sendAll(Ws.Text(s"display#$isCascade"))
    renderList()
    ujson.Obj("success" -> true, "err" -> "")
  }

  @cask.postJson("/user-filter")
  def applyFilter(filterStr: String): ujson.Obj = {
    println("Filter apply", filterStr)
    if (filterStr.nonEmpty) filter = Some(filterStr)
    else filter = None
    renderList()
    ujson.Obj("success" -> true, "err" -> "")
  }

  def messagesToJSON(msgs: List[Message]): List[ujson.Obj] = for (Message(id, to, username, msg) <- msgs) yield ujson.Obj("id" -> id, "username" -> username, "msg" -> msg)

  @cask.get("/messages")
  def getAllMessages(): Obj = ujson.Obj(
    "messages" -> messagesToJSON(db.getMessages)
  )

  @cask.get("/messages/:user")
  def getAllUserMessages(user: String): Obj = ujson.Obj(
    "messages" -> messagesToJSON(db.getMessages.filter(msg => msg.username.contains(user)))
  )

  @cask.postJson("/messages")
  def postMsg(to: String, name: String, msg: String): Obj = postChatMsg(to, name, msg)

  log.debug(s"Starting at $serverUrl")
  initialize()
}
