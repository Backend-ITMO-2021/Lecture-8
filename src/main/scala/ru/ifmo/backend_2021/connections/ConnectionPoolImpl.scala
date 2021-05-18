package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}

object WsConnectionPool {
  def apply(): ConnectionPool = new ConnectionPoolImpl()
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Set[WsChannelActor] = Set.empty[WsChannelActor]

  def getConnections: List[WsChannelActor] =
    synchronized(openConnections.toList)

  def send(event: Event): WsChannelActor => Unit = _.send(event)

  def sendAll(event: Event): Unit = sendAll(_ => event)

  def sendAll(event: WsChannelActor => Event): Unit = for (conn <- synchronized(openConnections)) send(event(conn))(conn)

  def addConnection(connection: WsChannelActor, handleEvent: PartialFunction[Event, Unit])
                   (implicit ac: castor.Context, log: Logger): WsActor = {
    synchronized {
      openConnections += connection
    }

    WsActor { event =>
      handleEvent.lift(event)

      if (event.isInstanceOf[Ws.Close]) {
        synchronized {
          openConnections -= connection
        }
      }
    }
  }

  def wsHandler(onConnect: WsChannelActor => Unit)
               (handleEvent: WsChannelActor => PartialFunction[Event, Unit])
               (implicit ac: castor.Context, log: Logger)
  : WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection, handleEvent(connection))
  }
}
