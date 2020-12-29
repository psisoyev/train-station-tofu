package com.psisoyev.train.station

import cats.implicits._
import cats.{ FlatMap, Show }
import com.psisoyev.train.station.Context.{ TraceId, UserId }
import io.estatico.newtype.macros.newtype
import tofu.generate.GenUUID
import tofu.logging.derivation.loggable
import tofu.logging.{ Loggable, LoggableContext }
import tofu.syntax.context.runContext
import tofu.{ HasContext, HasLocal, HasProvide, WithRun }

case class Context(traceId: TraceId, userId: UserId)

object Context {
  type WithCtx[F[_]] = HasLocal[F, Context]

  @newtype case class TraceId(value: String)
  @newtype case class UserId(value: String)

  implicit val ContextShow: Show[Context]         = Show.show(ctx => s"${ctx.userId.value}")
  implicit val ContextLoggable: Loggable[Context] = loggable.byShow("userId")

  implicit def loggableContext[F[_]: *[_] HasContext Context]: LoggableContext[F] =
    LoggableContext.of[F].instance

  def withUserContext[
    I[_]: GenUUID: FlatMap,
    F[_]: HasProvide[*[_], I, Context],
    T
  ](userId: UserId)(action: F[T]): I[T] =
    I.randomUUID.map(id => TraceId(id.toString)).flatMap { traceId =>
      runContext(action)(Context(traceId, userId))
    }

  def withSystemContext[
    I[_]: GenUUID: FlatMap,
    F[_]: HasProvide[*[_], I, Context],
    T
  ](action: F[T]): I[T] =
    withUserContext[I, F, T](UserId("system"))(action)
}
