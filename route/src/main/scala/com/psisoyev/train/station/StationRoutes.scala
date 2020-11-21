package com.psisoyev.train.station

import cats.{ Defer, Monad }
import cats.implicits._
import com.psisoyev.train.station.arrival.{ ArrivalValidator, Arrivals }
import com.psisoyev.train.station.arrival.Arrivals.Arrival
import com.psisoyev.train.station.arrival.ArrivalValidator.ArrivalError
import com.psisoyev.train.station.departure.Departures
import com.psisoyev.train.station.departure.Departures.{ Departure, DepartureError }
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, _ }
import org.http4s.circe._
import org.http4s.circe.CirceEntityEncoder._
import tofu.Handle
import tofu.syntax.handle._

class StationRoutes[F[_]: Monad: Defer: JsonDecoder](
  arrivals: Arrivals[F],
  arrivalValidator: ArrivalValidator[F],
  departures: Departures[F]
)(implicit
  E1: Handle[F, ArrivalError],
  E2: Handle[F, DepartureError]
) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "arrival"   =>
      val res = req
        .asJsonDecode[Arrival]
        .flatMap(arrivalValidator.validate)
        .flatMap(arrivals.register) *> Ok()

      E1.handleWith(res)(handleArrivalErrors)
    case req @ POST -> Root / "departure" =>
      val res = req
        .asJsonDecode[Departure]
        .flatMap(departures.register) *> Ok()

      E2.handleWith(res)(handleDepartureErrors)
  }

  def handleArrivalErrors: ArrivalError => F[Response[F]] = { case ArrivalError.UnexpectedTrain(id) =>
    BadRequest(s"Unexpected train $id")
  }

  def handleDepartureErrors: DepartureError => F[Response[F]] = { case DepartureError.UnexpectedDestination(city) =>
    BadRequest(s"Unexpected city $city")
  }
}
