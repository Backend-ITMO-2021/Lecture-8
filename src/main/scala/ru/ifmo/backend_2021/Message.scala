package ru.ifmo.backend_2021

case class Message(id: Int, parentId: Int, username: String, message: String) {
  def toFile: String = s"$id#$parentId#$username#$message"
}

object Message {
  def apply(fromString: String): Message = {
    fromString.split("#").toList match {
      case List(id, parentId, username, message) => Message(id.toInt, parentId.toInt, username, message)
    }
  }
}
