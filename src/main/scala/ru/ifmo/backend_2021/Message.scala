package ru.ifmo.backend_2021

import scalatags.Text
import scalatags.Text.all._

case class Message(id: String, to: Option[String], username: String, message: String) {
  def toFile: String = to match {
    case Some(toName) => s"$id#$toName#$username#$message"
    case None => s"$id#_#$username#$message"
  }
  def toListItemStr: Text.TypedTag[String] = {
    span(i(s"#${id.toString}"), " ", if(to.isDefined) s"-> #${to.get}" else "  ", "   ",  b(username), " ", message)
  }
}

object Message {
  def apply(fromString: String): Message = {
    val List(id, to, username, message) = fromString.split("#").toList
    Message(id, if (to != "_") Some(to) else None, username, message)
  }
}
