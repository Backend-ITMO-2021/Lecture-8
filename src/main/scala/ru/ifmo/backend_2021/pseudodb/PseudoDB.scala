package ru.ifmo.backend_2021.pseudodb

import ru.ifmo.backend_2021
import ru.ifmo.backend_2021.Message

import java.io.File
import scala.io.Source

object PseudoDB {
  def apply(filename: String, clean: Boolean): PseudoDB = {
    val db = new PseudoDB(filename)
    if (clean) db.clear()
    db
  }
}

class PseudoDB(filename: String) extends MessageDB {

  lazy val defaultMessages =
    List(
      Message(Some(1), "ventus976", "I don't particularly care which interaction they pick so long as it's consistent.", None),
      backend_2021.Message(Some(2), "XimbalaHu3", "Exactly, both is fine but do pick one.", None)
    )

  def clear(): Unit =
    new File(filename).delete()

  def createIfNotExists(): Unit = {
    val file = new File(filename)
    if (!file.exists()) {
      file.createNewFile()
      FileUtils.withFileWriter(filename)(_.write(defaultMessages.map(_.toFile).mkString("\n")))
    }
  }

  def getMessages: List[Message] = synchronized {
    createIfNotExists()
    FileUtils.withFileReader[List[Message]](filename)(_.map(Message(_)))
  }

  def addMessage(message: Message): Either[String, Message] = synchronized {
    createIfNotExists()

    val source = Source.fromFile(filename)
    try {
      val old = FileUtils.withFileReader[List[String]](filename)(identity)
      val oldMessages = old.map(Message(_))

      if (message.replyTo.nonEmpty && !oldMessages.exists(_.id == message.replyTo)) {
        return Left("replyTo")
      }

      val newMessage = message.id(oldMessages.map(_.id).max.getOrElse(0) + 1)
      val result = old :+ newMessage.toFile

      FileUtils.withFileWriter(filename)(_.write(result.mkString("\n")))

      return Right(newMessage)
    } finally {
      source.close()
    }
  }
}




