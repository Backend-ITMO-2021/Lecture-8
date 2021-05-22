package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}

import scala.collection.concurrent.TrieMap

object WsConnectionPool {
  def apply(messageFilter: Option[String] => String): ConnectionPool = new ConnectionPoolImpl(messageFilter)
}

class ConnectionPoolImpl(messageFilter: Option[String] => String) extends ConnectionPool {
  private val openConnections: TrieMap[WsChannelActor, Option[String]] = TrieMap()

  def getConnections: List[WsChannelActor] = openConnections.keys.toList

  def send(event: Event): WsChannelActor => Unit = _.send(event)

  def sendAll(event: Event): Unit = {
    for ((conn, _) <- openConnections) send(event)(conn)
  }

  def sendAllWithFilter(): Unit = {
    for {
      (conn, filterName) <- openConnections
    } send(Ws.Text(messageFilter(filterName)))(conn)
  }

  def addConnection(connection: WsChannelActor)(implicit ac: castor.Context, log: Logger): WsActor = {
    openConnections.addOne((connection, None))

    WsActor {
      case Ws.Text("") =>
        openConnections.update(connection, None)
        connection.send(Ws.Text(messageFilter(None)))
      case Ws.Text(name) =>
        openConnections.update(connection, Some(name))
        connection.send(Ws.Text(messageFilter(Some(name))))
      case Ws.Close(_, _) =>
        openConnections.remove(connection)
    }
  }

  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection)
  }
}
