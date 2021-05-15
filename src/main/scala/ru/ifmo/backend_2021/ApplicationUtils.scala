package ru.ifmo.backend_2021

import scalatags.Text
import scalatags.Text.all._
import scalatags.Text.tags2
import scalatags.generic
import scalatags.text.Builder
import ru.ifmo.backend_2021.pseudodb.{MessageDB, PseudoDB}

object ApplicationUtils {
  val styles = "https://cdn.jsdelivr.net/npm/bootstrap@5.0.1/dist/css/bootstrap.min.css"
  type Document = Text.all.doctype

  def getDB: MessageDB = PseudoDB(s"db.txt", clean = true)

  def messageList(messages: List[Message], filter: Option[String] = None): generic.Frag[Builder, String] = {

    def buildMessageThread(groupedMessages: Map[Option[Int], List[Message]], parentId: Option[Int] = None, depth: Int = 0): Frag = {
      val messages = groupedMessages.get(parentId)

      if (messages.isEmpty) {
        frag()
      } else {
        for (message <- messages.get)
          yield frag(renderMessage(message, depth), buildMessageThread(groupedMessages, Some(message.id), depth + 1))
      }
    }

    def renderMessage(message: Message, depth: Int = 0): generic.Frag[Builder, String] = {
      val Message(id, name, msg, _) = message
      p(span(css("white-space") := "pre-wrap", "    " * depth), span(cls := "text-secondary", s"#$id"), " ", b(name), " ", msg)
    }

    if (filter.isDefined) {
      val filtered = messages.filter(_.username == filter.get)
      frag(for (msg <- filtered) yield renderMessage(msg))
    } else {
      val messagesGroupedByRoot = messages.groupBy(_.replyTo)
      buildMessageThread(messagesGroupedByRoot)
    }
  }
}
