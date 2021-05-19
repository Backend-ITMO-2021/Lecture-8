package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}
import ru.ifmo.backend_2021.RedditApplication.filterMessageList

object WsConnectionPool {
  def apply(): ConnectionPool = new ConnectionPoolImpl()
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Set[WsChannelActor] = Set.empty[WsChannelActor]
  def getConnections: List[WsChannelActor] =
    synchronized(openConnections.toList)
  def send(event: Event): WsChannelActor => Unit = _.send(event)
  def sendAll(event: Event): Unit = for (conn <- synchronized(openConnections)) send(event)(conn)
  def addConnection(connection: WsChannelActor)(implicit ac: castor.Context, log: Logger): WsActor = {
    synchronized {
      openConnections += connection
    }
    WsActor {
      case cask.Ws.Text(data) => connection.send(cask.Ws.Text(filterMessageList(Option.unless(data == "")(data)).render))
      case Ws.Close(_, _) =>
      synchronized {
        openConnections -= connection
      }
    }
  }
  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection)
  }
}
