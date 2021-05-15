package ru.ifmo.backend_2021

case class Message(id: Int, username: String, message: String, replyTo: Option[Int]) {
  def toFile: String = s"$id#$username#$message#${replyTo.getOrElse(0)}"
}

object Message {
  def apply(fromString: String): Message = {
    val List(id, username, message, replyTo) = fromString.split("#").toList
    Message(id.toInt, username, message, Option.unless(replyTo == "0")(replyTo.toInt))
  }
}
