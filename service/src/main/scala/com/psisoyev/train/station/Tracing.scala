package com.psisoyev.train.station

import cats.FlatMap
import tofu.generate.GenUUID
import tofu.logging.Logging
import tofu.syntax.context.{ askF, runContext }
import tofu.syntax.monadic._
import tofu.{ HasLocal, HasProvide }

trait Tracing[F[_]] {
  def traced[A](opName: String)(fa: F[A]): F[A]
}
object Tracing      {
  type WithCtx[F[_]] = HasLocal[F, Context]

  def make[F[_]: FlatMap: Logging: WithCtx]: Tracing[F] = new Tracing[F] {
    def traced[A](opName: String)(fa: F[A]): F[A] =
      askF[F]((ctx: Context) => F.info(s"[Tracing][traceId=${ctx.traceId}] $opName") *> fa)
  }

  object ops {
    implicit class TracingOps[F[_], A](private val fa: F[A]) extends AnyVal {
      def traced(opName: String)(implicit F: Tracing[F]): F[A] = F.traced(opName)(fa)
    }
  }
}
