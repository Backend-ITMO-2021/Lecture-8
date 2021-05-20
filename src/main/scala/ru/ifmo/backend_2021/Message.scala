package ru.ifmo.backend_2021

import scalatags.Text
import scalatags.Text.all._

case class Message(username: String, message: String, id: Int, parentId: Int, depth: Int) {
  def toFile: String = s"$username#$message#$id#$parentId#$depth"

  def getHtmlMessage(position: Int): Text.TypedTag[String] =
    p(span(css("white-space") := "pre-wrap", "     ".repeat(depth)), s"#$position", " ", b(username), " ", message)
}

object Message {
  def apply(fromString: String): Message = {
    val List(username, message, id, parentId, depth) = fromString.split("#").toList
    Message(username, message, id.toInt, parentId.toInt, depth.toInt)
  }
}
