package ru.ifmo.backend_2021

case class Message(username: String, message: String) {
  def toFile: String = s"$username#$message"
}

object Message {
  def apply(fromString: String): Message = {
    val List(username, message) = fromString.split("#").toList
    Message(username, message)
  }
}
