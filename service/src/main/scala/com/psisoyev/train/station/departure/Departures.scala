package com.psisoyev.train.station.departure

import cats.{ Applicative, FlatMap, Monad }
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station._
import com.psisoyev.train.station.departure.Departures.DepartureError.UnexpectedDestination
import com.psisoyev.train.station.departure.Departures.Departure
import cr.pulsar.Producer
import derevo.derive
import derevo.tagless.applyK
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import tofu.Raise
import tofu.generate.GenUUID
import tofu.higherKind.Mid
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.syntax.monoid.TofuSemigroupOps
import tofu.syntax.raise._

import scala.util.control.NoStackTrace

@derive(applyK)
trait Departures[F[_]] {
  def register(departure: Departure): F[Departed]
}

object Departures {
  sealed trait DepartureError extends NoStackTrace
  object DepartureError {
    case class UnexpectedDestination(city: City) extends DepartureError
  }

  case class Departure(id: TrainId, to: To, time: Expected, actual: Actual)
  object Departure {
    implicit val departureDecoder: Decoder[Departure] = deriveDecoder
  }

  private class Logger[F[_]: FlatMap: Logging] extends Departures[Mid[F, *]] {
    def register(departure: Departure): Mid[F, Departed] = { registation =>
      F.info(s"Registering $departure") *> registation <* F.info(s"Train ${departure.id} successfully departed")
    }
  }

  private class Publisher[F[_]: Monad](producer: Producer[F, Event]) extends Departures[Mid[F, *]] {
    def register(departure: Departure): Mid[F, Departed] =
      _.flatTap(producer.send_)
  }

  private class Validator[F[_]: Monad: Raise[*[_], DepartureError]](connectedTo: List[City]) extends Departures[Mid[F, *]] {
    def register(departure: Departure): Mid[F, Departed] = { registration =>
      val destination = departure.to.city

      connectedTo
        .find(_ == destination)
        .orRaise[F](UnexpectedDestination(destination)) *> registration
    }
  }

  private class Impl[F[_]: Applicative: GenUUID](city: City) extends Departures[F] {
    override def register(departure: Departure): F[Departed] =
      F.randomUUID.map { id =>
        Departed(
          EventId(id),
          departure.id,
          From(city),
          departure.to,
          departure.time,
          departure.actual.toTimestamp
        )
      }
  }

  def make[F[_]: Monad: GenUUID: Logging: Raise[*[_], DepartureError]](
    city: City,
    connectedTo: List[City],
    producer: Producer[F, Event]
  ): Departures[F] = {
    val service = new Impl[F](city)

    val logging: Departures[Mid[F, *]]    = new Logger[F]
    val publishing: Departures[Mid[F, *]] = new Publisher[F](producer)
    val validation: Departures[Mid[F, *]] = new Validator[F](connectedTo)

    (logging |+| validation |+| publishing).attach(service)
  }
}
