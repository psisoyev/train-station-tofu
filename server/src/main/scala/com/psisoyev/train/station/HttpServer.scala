package com.psisoyev.train.station

import cats.effect.ConcurrentEffect
import com.psisoyev.train.station.Main.{ platform, Routes }
import org.http4s.server.blaze.BlazeServerBuilder
import cats.effect.Temporal

object HttpServer {
  def start[Init[_]: ConcurrentEffect: Temporal](
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
