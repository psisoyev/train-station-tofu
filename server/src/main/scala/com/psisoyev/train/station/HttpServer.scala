package com.psisoyev.train.station

import cats.effect.{ ConcurrentEffect, Timer }
import com.psisoyev.train.station.Main.{ platform, Routes }
import org.http4s.server.blaze.BlazeServerBuilder

object HttpServer {
  def start[Init[_]: ConcurrentEffect: Timer](
    config: Config,
    routes: Routes[Init]
  ): Init[Unit] =
    BlazeServerBuilder[Init](platform.executor.asEC)
      .bindHttp(config.httpPort.value, "0.0.0.0")
      .withHttpApp(routes)
      .serve
      .compile
      .drain
}
