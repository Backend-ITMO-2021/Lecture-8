package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}
import ru.ifmo.backend_2021.ApplicationUtils._
import ru.ifmo.backend_2021.RedditApplication

object WsConnectionPool {
  def apply(): ConnectionPool = new ConnectionPoolImpl()
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Set[WsChannelActor] = Set.empty[WsChannelActor]
  private var openConnectionsFilters: Map[WsChannelActor, String] = Map.empty[WsChannelActor, String]

  def getConnections: List[WsChannelActor] =
    synchronized(openConnections.toList)

  def send(event: Event): WsChannelActor => Unit = _.send(event)

  def sendAll(actorToEvent: WsChannelActor => Event): Unit = for (conn <- synchronized(openConnections)) send(actorToEvent(conn))(conn)

  def addConnection(connection: WsChannelActor)(implicit ac: castor.Context, log: Logger): WsActor = {
    synchronized {
      openConnections += connection
    }
    WsActor {
      case cask.Ws.Text(data) => {
        openConnectionsFilters += connection -> data
        connection.send(cask.Ws.Text(RedditApplication.messageList(Option.unless(data == "")(data)).render))
      }
      case Ws.Close(_, _) => synchronized {
        openConnectionsFilters -= connection
        openConnections -= connection
      }
    }
  }

  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection)
  }

  override def getFilter(wsChannelActor: WsChannelActor): Option[String] = openConnectionsFilters.get(wsChannelActor)
}
