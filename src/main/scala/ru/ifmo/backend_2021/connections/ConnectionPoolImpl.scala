package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}
import ru.ifmo.backend_2021.RedditApplication.messageList

object WsConnectionPool {
  val connection = new ConnectionPoolImpl()
  def apply(): ConnectionPool = connection
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Map[WsChannelActor, String] = Map.empty[WsChannelActor, String]

  def getConnections: List[WsChannelActor] = synchronized(openConnections.keys.toList)
  def send(event: Event): WsChannelActor => Unit = _.send(event)
  def sendAll(event: WsChannelActor => Event): Unit = for (conn <- synchronized(openConnections)) send(event(conn._1))(conn._1)

  def addConnection(connection: WsChannelActor)(implicit ac: castor.Context, log: Logger): WsActor = {
    synchronized {
      openConnections += connection -> ""
    }
    WsActor {
      case cask.Ws.Text(data) => {
        openConnections += connection -> data
        connection.send(cask.Ws.Text(messageList(Option.unless(data == "")(data)).render))
      }

      case Ws.Close(_, _) => synchronized { openConnections -= connection }
    }
  }

  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection)
  }

  override def getFilter(wsChannelActor: WsChannelActor): Option[String] = openConnections.get(wsChannelActor)
}