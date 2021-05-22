package ru.ifmo.backend_2021.connections

import cask.WsChannelActor
import cask.endpoints.WsHandler
import cask.util.Logger
import cask.util.Ws.Event

trait ConnectionPool {
  def send(event: Event): WsChannelActor => Unit
  def sendAll(actorToEvent: WsChannelActor => Event): Unit
  def wsHandler(onConnect: WsChannelActor => Unit)(implicit ac: castor.Context, log: Logger): WsHandler
  def getFilter(wsChannelActor: WsChannelActor): Option[String]
}
