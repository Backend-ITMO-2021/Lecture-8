package ru.ifmo.backend_2021

import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ApplicationUtils.messageList

case class AppAPI()(implicit cc: castor.Context, log: cask.Logger) extends cask.Routes {
  val db = ApplicationUtils.getDB
  val connectionPool: ConnectionPool = WsConnectionPool()

  @cask.postJson("/messages")
  def postMessage(username: String, message: String, replyTo: Int = 0): ujson.Obj = {
    log.debug("API: /messages (POST) ", username, message, replyTo)

    synchronized {
      db.appendMessage(username, message, Option.unless(replyTo == 0)(replyTo))
      connectionPool.sendAll(Ws.Text(messageList(db.getMessages).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/messages/:username")
  def userMessages(username: String): ujson.Obj = {
    log.debug(s"API: /messages/$username (GET)")

    synchronized {
      val filtered = db.getMessages.filter(_.username == username)
      ujson.Obj("messages" -> filtered.map(_.message))
    }
  }

  @cask.get("/messages")
  def allMessages(): ujson.Obj = {
    log.debug(s"API: /messages (GET)")

    synchronized {
      ujson.Obj("messages" -> db.getMessages.map(msg => {
        val obj = ujson.Obj("id" -> msg.id, "username" -> msg.username, "message" -> msg.message)
        if (msg.replyTo.isDefined) obj("replyTo") = msg.replyTo.get
        obj
      }))
    }
  }

  initialize()
}