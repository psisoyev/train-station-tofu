package com.psisoyev.train.station.departure

import cats.{ Applicative, FlatMap, Monad }
import com.psisoyev.train.station.City
import com.psisoyev.train.station.Context._
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.arrival.ExpectedTrains
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import derevo.derive
import derevo.tagless.applyK
import tofu.higherKind.Mid
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.syntax.context.askF
import com.psisoyev.train.station.Context

@derive(applyK)
trait DepartureTracker[F[_]] {
  def save(e: Departed): F[Unit]
}

object DepartureTracker {

  private class Logger[F[_]: FlatMap: Logging: WithCtx] extends DepartureTracker[Mid[F, *]] {
    def save(e: Departed): Mid[F, Unit] =
      _ *> askF[F] { ctx: Context =>
        F.info(s"[${ctx.userId}] ${e.to.city} is expecting ${e.trainId} from ${e.from} at ${e.expected}")
      }
  }

  private class Impl[F[_]: Applicative](city: City, expectedTrains: ExpectedTrains[F]) extends DepartureTracker[F] {
    def save(e: Departed): F[Unit] =
      expectedTrains
        .update(e.trainId, ExpectedTrain(e.from, e.expected))
        .whenA(e.to.city == city)
  }

  def make[F[_]: Monad: Logging: WithCtx](
    city: City,
    expectedTrains: ExpectedTrains[F]
  ): DepartureTracker[F] = {
    val service = new Impl[F](city, expectedTrains)

    (new Logger[F]).attach(service)
  }
}
