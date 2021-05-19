package ru.ifmo.backend_2021

case class Message(id: Int, username: String, message: String, parent: Option[Int]) {
  def toFile: String = s"$id#$username#$message#${parent.getOrElse(0)}"
}

object Message {
  def apply(fromString: String): Message = {
    val List(id, username, message, parent) = fromString.split("#").toList
    Message(id.toInt, username, message, Option.unless(parent == "0")(parent.toInt))
  }
}
