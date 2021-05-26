package ru.ifmo.backend_2021.connections

import cask.WsChannelActor
import cask.endpoints.WsHandler
import cask.util.Logger
import cask.util.Ws.Event

case class ConnectionParams(userFilter: Option[String], isCascade: Boolean = false)

trait ConnectionPool {
  def send(event: Event): WsChannelActor => Unit
  def sendAll(eventActor: WsChannelActor => Event): Unit
  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler
  def getConnectionParams(channelActor: WsChannelActor): Option[ConnectionParams]
}
