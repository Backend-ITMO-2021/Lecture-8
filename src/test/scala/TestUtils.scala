import ujson.{Readable, Value}

object TestUtils {
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = io.undertow.Undertow.builder
      .addHttpListener(8081, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8081")
      finally server.stop()
    res
  }

  def readJsonAndAssert(r: Readable)(f: Value.Value => Boolean): Unit =
    assert(f(ujson.read(r)))
}
