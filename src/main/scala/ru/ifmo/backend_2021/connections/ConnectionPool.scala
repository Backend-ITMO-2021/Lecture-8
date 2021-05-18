package ru.ifmo.backend_2021.connections

import cask.WsChannelActor
import cask.endpoints.WsHandler
import cask.util.Logger
import cask.util.Ws.Event

trait ConnectionPool {
  def send(event: Event): WsChannelActor => Unit
  def sendAll(event: Event): Unit
  def sendAll(event: WsChannelActor => Event): Unit
  def wsHandler(onConnect: WsChannelActor => Unit)
               (handleEvent: WsChannelActor => PartialFunction[Event, Unit] = _ => _ => ())
               (implicit ac: castor.Context, log: Logger): WsHandler
}
