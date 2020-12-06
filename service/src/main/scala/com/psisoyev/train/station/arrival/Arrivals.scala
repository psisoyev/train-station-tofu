package com.psisoyev.train.station.arrival

import cats.{ Applicative, FlatMap, Monad }
import com.psisoyev.train.station.Event.Arrived
import com.psisoyev.train.station.arrival.ArrivalValidator.ValidatedArrival
import com.psisoyev.train.station.{ Actual, City, EventId, To, TrainId }
import derevo.derive
import derevo.tagless.applyK
import io.circe.Decoder
import io.circe.generic.semiauto._
import tofu.generate.GenUUID
import tofu.higherKind.Mid
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait Arrivals[F[_]] {
  def register(arrival: ValidatedArrival): F[Arrived]
}

object Arrivals {
  case class Arrival(trainId: TrainId, time: Actual)
  object Arrival {
    implicit val arrivalDecoder: Decoder[Arrival] = deriveDecoder
  }

  private class Logger[F[_]: FlatMap: Logging] extends Arrivals[Mid[F, *]] {
    def register(arrival: ValidatedArrival): Mid[F, Arrived] = { registration =>
      F.info(s"Registering $arrival") *> registration <* F.info(s"Train ${arrival.trainId} successfully arrived")
    }
  }

  private class Cleaner[F[_]: Monad](expectedTrains: ExpectedTrains[F]) extends Arrivals[Mid[F, *]] {
    def register(arrival: ValidatedArrival): Mid[F, Arrived] =
      _.flatTap(_ => expectedTrains.remove(arrival.trainId))
  }

  private class Impl[F[_]: Applicative: GenUUID](city: City) extends Arrivals[F] {
    override def register(arrival: ValidatedArrival): F[Arrived] =
      F.randomUUID.map { id =>
        Arrived(
          EventId(id),
          arrival.trainId,
          arrival.expectedTrain.from,
          To(city),
          arrival.expectedTrain.time,
          arrival.time.toTimestamp
        )
      }
  }

  def make[F[_]: Monad: GenUUID: Logging](
    city: City,
    expectedTrains: ExpectedTrains[F]
  ): Arrivals[F] = {
    val service = new Impl[F](city)

    val logger: Arrivals[Mid[F, *]]  = new Logger[F]
    val cleaner: Arrivals[Mid[F, *]] = new Cleaner[F](expectedTrains)

    (logger |+| cleaner).attach(service)
  }
}
