package com.psisoyev.train.station.departure

import cats.{ Applicative, FlatMap, Monad }
import com.psisoyev.train.station.City
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.arrival.ExpectedTrains
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import derevo.derive
import derevo.tagless.applyK
import io.chrisdavenport.log4cats.Logger
import tofu.higherKind.Mid
import tofu.syntax.monadic._

@derive(applyK)
trait DepartureTracker[F[_]] {
  def save(e: Departed): F[Unit]
}

object DepartureTracker {

  private class Log[F[_]: FlatMap: Logger] extends DepartureTracker[Mid[F, *]] {
    def save(e: Departed): Mid[F, Unit] =
      _ *> F.info(s"${e.to.city} is expecting ${e.trainId} from ${e.from} at ${e.expected}")
  }

  private class Impl[F[_]: Applicative](city: City, expectedTrains: ExpectedTrains[F]) extends DepartureTracker[F] {
    def save(e: Departed): F[Unit] =
      expectedTrains
        .update(e.trainId, ExpectedTrain(e.from, e.expected))
        .whenA(e.to.city == city)
  }

  def make[F[_]: Monad: Logger](
    city: City,
    expectedTrains: ExpectedTrains[F]
  ): DepartureTracker[F] = {
    val service = new Impl[F](city, expectedTrains)

    (new Log[F]).attach(service)
  }
}
