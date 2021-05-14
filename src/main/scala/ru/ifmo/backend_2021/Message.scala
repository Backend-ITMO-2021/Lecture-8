package ru.ifmo.backend_2021

case class Message(id: Int, username: String, message: String, parentId: Option[Int]) {
  def toFile: String = s"$id#$username#$message#${parentId.getOrElse(0)}"
}

case class ChildMessage(depth: Int, message: Message)

object Message {
  def apply(fromString: String): Message = {
    val List(id, username, message, parentId) = fromString.split("#").toList
    Message(id.toInt, username, message, if (parentId == "0") None else Some(parentId.toInt))
  }
}
