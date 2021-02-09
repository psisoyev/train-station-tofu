package com.psisoyev.train.station.arrival

import cats.{ FlatMap, Functor, Monad }
import com.psisoyev.train.station.Event.Arrived
import com.psisoyev.train.station.Tracing.ops.TracingOps
import com.psisoyev.train.station.arrival.ArrivalValidator.ValidatedArrival
import com.psisoyev.train.station.{ Actual, City, EventId, To, Tracing, TrainId }
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

  private class Log[F[_]: FlatMap: Logging] extends Arrivals[Mid[F, *]] {
    def register(arrival: ValidatedArrival): Mid[F, Arrived] = { registration =>
      val before = F.info(s"Registering $arrival")
      val after  = F.info(s"Train ${arrival.trainId} successfully arrived")

      before *> registration <* after
    }
  }

  private class Trace[F[_]: Tracing] extends Arrivals[Mid[F, *]] {
    def register(arrival: ValidatedArrival): Mid[F, Arrived] = _.traced("train arrival: register")
  }

  private class Clean[F[_]: Monad](expectedTrains: ExpectedTrains[F]) extends Arrivals[Mid[F, *]] {
    def register(arrival: ValidatedArrival): Mid[F, Arrived] =
      _.flatTap(_ => expectedTrains.remove(arrival.trainId))
  }

  private class Impl[F[_]: Functor: GenUUID](city: City) extends Arrivals[F] {
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

  def make[F[_]: Monad: GenUUID: Logging: Tracing](
    city: City,
    expectedTrains: ExpectedTrains[F]
  ): Arrivals[F] = {
    val service = new Impl[F](city)

    val log: Arrivals[Mid[F, *]]   = new Log[F]
    val trace: Arrivals[Mid[F, *]] = new Trace[F]
    val clean: Arrivals[Mid[F, *]] = new Clean[F](expectedTrains)

    (log |+| trace |+| clean).attach(service)
  }
}
