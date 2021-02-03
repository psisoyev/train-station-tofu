package com.psisoyev.train.station

import cats.FlatMap
import com.psisoyev.train.station.Context.WithCtx
import io.chrisdavenport.log4cats.StructuredLogger

trait Logging[F[_]] {
  def info(str: String): F[Unit]
}

object Logging {
  def make[F[_]: StructuredLogger: WithCtx: FlatMap]: Logging[F] = new Logging[F] {
    override def info(str: String): F[Unit] = F.askF { ctx =>
      F.info(Map("userId" -> ctx.userId.value))(str)
    }
  }
}
