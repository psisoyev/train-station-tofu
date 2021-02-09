package com.psisoyev.train.station

import cats.FlatMap
import cats.implicits._
import com.psisoyev.train.station.Context.{ TraceId, UserId }
import derevo.derive
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import tofu.generate.GenUUID
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import tofu.syntax.context.runContext
import tofu.{ WithContext, WithProvide }

@derive(loggable)
case class Context(traceId: TraceId, userId: UserId)

object Context {
  type WithCtx[F[_]]       = WithContext[F, Context]
  type RunsCtx[F[_], G[_]] = WithProvide[F, G, Context]

  @newtype case class TraceId(value: String)
  @newtype case class UserId(value: String)

  implicit def coercibleLoggable[A: Coercible[B, *], B: Loggable]: Loggable[A] =
    Loggable[B].contramap[A](_.asInstanceOf[B])

  def withUserContext[
    I[_]: GenUUID: FlatMap,
    F[_]: RunsCtx[*[_], I],
    T
  ](userId: UserId)(action: F[T]): I[T] =
    I.randomUUID.map(id => TraceId(id.toString)).flatMap { traceId =>
      runContext(action)(Context(traceId, userId))
    }

  def withSystemContext[
    I[_]: GenUUID: FlatMap,
    F[_]: RunsCtx[*[_], I],
    T
  ](action: F[T]): I[T] =
    withUserContext[I, F, T](UserId("system"))(action)
}
