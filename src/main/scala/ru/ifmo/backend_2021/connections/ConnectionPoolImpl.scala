package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}
import ru.ifmo.backend_2021.RedditApplication.{connectionPool, messageList, userFilter}
import scalatags.Text.all.s

object WsConnectionPool {
  def apply(): ConnectionPool = new ConnectionPoolImpl()
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Set[WsChannelActor] = Set.empty[WsChannelActor]
  private var openConnectionsParams = Map.empty[WsChannelActor, ConnectionParams]

  override def getConnectionParams(channelActor: WsChannelActor): Option[ConnectionParams] = openConnectionsParams.get(channelActor)

  def getConnections: List[WsChannelActor] =
    synchronized(openConnections.toList)
  def send(event: Event): WsChannelActor => Unit = _.send(event)
  def sendAll(eventActor: WsChannelActor => Event): Unit = for (conn <- synchronized(openConnections)) send(eventActor(conn))(conn)
  def addConnection(connection: WsChannelActor)(implicit ac: castor.Context, log: Logger): WsActor = {
    synchronized {
      openConnections += connection
    }
    WsActor {
      case Ws.Text(_) =>
        openConnectionsParams += connection -> ConnectionParams(None)
        connection.send(cask.Ws.Text(messageList(userFilter(None)).render))
      case Ws.Close(_, _) => synchronized {
        openConnectionsParams -= connection
        openConnections -= connection
      }
    }
  }
  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection)
    WsActor {
      case cask.Ws.Text(data) =>
        println(data)
        val prevConnParams = getConnectionParams(connection) match {
          case Some(value) => value
          case None => ConnectionParams(None)
        }
        if (data.contains("filter=")) {
          if (data.replace("filter=", "").nonEmpty) {
            openConnectionsParams += connection -> ConnectionParams(Some(data.replace("filter=", "")), prevConnParams.isCascade)
          } else {
            openConnectionsParams += connection -> ConnectionParams(None, prevConnParams.isCascade);
          }
        }

        if (data.contains("changeDisplay?")) {
          openConnectionsParams += connection -> ConnectionParams(prevConnParams.userFilter, !prevConnParams.isCascade);
          connection.send(cask.Ws.Text(s"display#${!prevConnParams.isCascade}"))
        }

        val connParams = getConnectionParams(connection) match {
          case Some(value) => value
          case None => ConnectionParams(None)
        }
        connection.send(cask.Ws.Text(messageList(userFilter(connParams.userFilter), connParams.isCascade).render))
    }
  }
}
