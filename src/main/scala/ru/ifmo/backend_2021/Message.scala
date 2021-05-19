package ru.ifmo.backend_2021

case class Message(id: String, username: String, message: String, idParent: String = "none") {
  def toFile: String = s"$id#$username#$message#$idParent"
}

object Message {
  def apply(fromString: String): Message = {
    val List(id, username, message, idParent) = fromString.split("#").toList
    Message(id, username, message, idParent)
  }
}
