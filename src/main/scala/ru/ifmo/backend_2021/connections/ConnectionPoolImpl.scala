package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}
import ru.ifmo.backend_2021.ApplicationUtils._

object WsConnectionPool {
  val connection = new ConnectionPoolImpl()
  def apply(): ConnectionPool = connection
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Set[WsChannelActor] = Set.empty[WsChannelActor]
  val db = getDB

  def getConnections: List[WsChannelActor] = synchronized(openConnections.toList)
  def send(event: Event): WsChannelActor => Unit = _.send(event)
  def sendAll(event: Event): Unit = for (conn <- synchronized(openConnections)) send(event)(conn)

  def addConnection(connection: WsChannelActor)(implicit ac: castor.Context, log: Logger): WsActor = {
    synchronized {
      openConnections += connection
    }
    WsActor {
      case cask.Ws.Text(data) => connection.send(cask.Ws.Text(messageList(db.getMessages, Option.unless(data == "")(data)).render))
      case Ws.Close(_, _) => synchronized { openConnections -= connection }
    }
  }

  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection)
  }
}
