package ru.ifmo.backend_2021

case class Message(id: Option[Int], username: String, message: String, replyTo: Option[Int]) {

  def id(id: Int): Message = Message(Some(id), username, message, replyTo)

  def toFile: String = s"${id.get}#$username#$message#${replyTo.getOrElse(-1)}"
}

object Message {

  def apply(fromString: String): Message = {
    val List(id, username, message, replyTo) = fromString.split("#").toList

    Message(Some(id.toInt), username, message, Some(replyTo.toInt).filter(_ > 0))
  }
}
