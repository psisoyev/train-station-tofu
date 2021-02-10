package com.psisoyev.train.station.departure

import cats.{ FlatMap, Functor, Monad }
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.Tracing.ops.TracingOps
import com.psisoyev.train.station._
import com.psisoyev.train.station.departure.Departures.Departure
import com.psisoyev.train.station.departure.Departures.DepartureError.UnexpectedDestination
import derevo.derive
import derevo.tagless.applyK
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import tofu.generate.GenUUID
import tofu.higherKind.Mid
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.syntax.monoid.TofuSemigroupOps
import tofu.syntax.raise._
import tofu.{ Handle, Raise }

import scala.util.control.NoStackTrace

@derive(applyK)
trait Departures[F[_]] {
  def register(departure: Departure): F[Departed]
}

object Departures {
  sealed trait DepartureError extends NoStackTrace
  object DepartureError {
    type Handling[F[_]] = Handle[F, DepartureError]
    type Raising[F[_]]  = Raise[F, DepartureError]

    case class UnexpectedDestination(city: City) extends DepartureError
  }

  case class Departure(id: TrainId, to: To, time: Expected, actual: Actual)
  object Departure {
    implicit val departureDecoder: Decoder[Departure] = deriveDecoder
  }

  private class Log[F[_]: FlatMap: Logging] extends Departures[Mid[F, *]] {
    def register(departure: Departure): Mid[F, Departed] = { registration =>
      val before = F.info(s"Registering $departure")
      val after  = F.info(s"Train ${departure.id.value} successfully departed")

      before *> registration <* after
    }
  }

  private class Trace[F[_]: Tracing] extends Departures[Mid[F, *]] {
    def register(departure: Departure): Mid[F, Departed] = _.traced("train departure: register")
  }

  private class Validate[F[_]: Monad: DepartureError.Raising](connectedTo: List[City]) extends Departures[Mid[F, *]] {
    def register(departure: Departure): Mid[F, Departed] = { registration =>
      val destination = departure.to.city

      connectedTo
        .find(_ == destination)
        .orRaise(UnexpectedDestination(destination)) *> registration
    }
  }

  private class Impl[F[_]: Functor: GenUUID](city: City) extends Departures[F] {
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

  def make[F[_]: Monad: GenUUID: Logging: DepartureError.Raising: Tracing](
    city: City,
    connectedTo: List[City]
  ): Departures[F] = {
    val service = new Impl[F](city)

    val trace: Departures[Mid[F, *]]    = new Trace[F]
    val log: Departures[Mid[F, *]]      = new Log[F]
    val validate: Departures[Mid[F, *]] = new Validate[F](connectedTo)

    (log |+| validate |+| trace).attach(service)
  }
}
