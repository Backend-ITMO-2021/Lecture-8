package ru.ifmo.backend_2021.pseudodb

import java.io.{BufferedWriter, File, PrintWriter}
import scala.io.Source

object FileUtils {
  def withFileReader[T](fileName: String)(callback: List[String] => T):T  = {
    val source = Source.fromFile(fileName)
    try {
      callback(source.getLines().toList)
    }
    finally {
      source.close()
    }
  }

  def withFileWriter(fileName: String)(callback: BufferedWriter => Unit): Unit = {
    val fileWriter = new BufferedWriter(new PrintWriter(new File(fileName)))
    try {
      callback(fileWriter)
    }
    finally {
      fileWriter.close()
    }
  }
}
