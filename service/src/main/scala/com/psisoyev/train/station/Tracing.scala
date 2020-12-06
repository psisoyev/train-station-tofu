package com.psisoyev.train.station

import cats.FlatMap
import tofu.{ HasLocal, HasProvide }
import tofu.generate.GenUUID
import tofu.logging.Logging
import tofu.syntax.context.{ askF, runContext }
import tofu.syntax.monadic._

trait Tracing[F[_]] {
  def traced[A](opName: String)(fa: F[A]): F[A]
}
object Tracing      {
  type WithCtx[F[_]] = HasLocal[F, Ctx]

  def make[F[_]: FlatMap: Logging: WithCtx]: Tracing[F] = new Tracing[F] {
    def traced[A](opName: String)(fa: F[A]): F[A] =
      askF[F]((ctx: Ctx) => F.info(s"[Tracing][traceId=${ctx.traceId}] $opName") *> fa)
  }

  object ops {
    implicit class TracingOps[F[_], A](private val fa: F[A]) extends AnyVal {
      def traced(opName: String)(implicit F: Tracing[F]): F[A] = F.traced(opName)(fa)
    }
  }

  def withNewTrace[I[_]: GenUUID: FlatMap, F[_]: HasProvide[*[_], I, Ctx], T](action: F[T]): I[T] =
    I.randomUUID.map(id => TraceId(id.toString)).flatMap { traceId =>
      runContext(action)(Ctx(traceId))
    }
}
