package com.psisoyev.train.station

import cats.FlatMap
import com.psisoyev.train.station.Context._
import io.chrisdavenport.log4cats.Logger
import tofu.syntax.context.askF
import tofu.syntax.monadic._

trait Tracing[F[_]] {
  def traced[A](opName: String)(fa: F[A]): F[A]
}
object Tracing      {
  def make[F[_]: FlatMap: Logger: WithCtx]: Tracing[F] = new Tracing[F] {
    def traced[A](opName: String)(fa: F[A]): F[A] =
      askF[F]((ctx: Context) => F.info(s"[Tracing][traceId=${ctx.traceId}] $opName") *> fa)
  }

  object ops {
    implicit class TracingOps[F[_], A](private val fa: F[A]) extends AnyVal {
      def traced(opName: String)(implicit F: Tracing[F]): F[A] = F.traced(opName)(fa)
    }
  }
}
