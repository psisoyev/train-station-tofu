package com.psisoyev.train.station

import cats.FlatMap
import cats.implicits._
import com.psisoyev.train.station.Context.{ TraceId, UserId }
import io.estatico.newtype.macros.newtype
import tofu.HasProvide
import tofu.generate.GenUUID
import tofu.syntax.context.runContext

case class Context(traceId: TraceId, userId: UserId)

object Context {
  @newtype case class TraceId(value: String)
  @newtype case class UserId(value: String)

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
