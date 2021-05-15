package ru.ifmo.backend_2021

object RedditApplication extends cask.Main {
  val allRoutes = Seq(AppStatic(), AppAPI())

  val serverUrl = s"http://$host:$port"
  log.debug(s"Starting at $serverUrl")
}
