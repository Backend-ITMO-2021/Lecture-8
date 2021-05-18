package ru.ifmo.backend_2021.pseudodb

import ru.ifmo.backend_2021.Message

trait MessageDB {
  def getMessages: List[Message]
  def addMessage(message: Message): Either[String, Message]
}
