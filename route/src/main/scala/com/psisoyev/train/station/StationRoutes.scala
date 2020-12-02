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
import tofu.{ Handle, WithRun }
import tofu.syntax.context._

class StationRoutes[I[_]: Monad: Defer: JsonDecoder, F[_]: WithRun[*[_], I, Ctx]](
  arrivals: Arrivals[F],
  arrivalValidator: ArrivalValidator[F],
  departures: Departures[F]
)(implicit
  E1: Handle[I, ArrivalError],
  E2: Handle[I, DepartureError]
) extends Http4sDsl[I] {
  val routes: HttpRoutes[I] = HttpRoutes.of[I] {
    case req @ POST -> Root / "arrival"   =>
      val res = req
        .asJsonDecode[Arrival] // TODO re-use context
        .flatMap(arrival => runContext(arrivalValidator.validate(arrival))(Ctx(Map())))
        .flatMap(arrival => runContext(arrivals.register(arrival))(Ctx(Map()))) *> Ok()

      E1.handleWith(res)(handleArrivalErrors)
    case req @ POST -> Root / "departure" =>
      val res = req
        .asJsonDecode[Departure]
        .flatMap(departure => runContext(departures.register(departure))(Ctx(Map()))) *> Ok()

      E2.handleWith(res)(handleDepartureErrors)
  }

  def handleArrivalErrors: ArrivalError => I[Response[I]] = { case ArrivalError.UnexpectedTrain(id) =>
    BadRequest(s"Unexpected train $id")
  }

  def handleDepartureErrors: DepartureError => I[Response[I]] = { case DepartureError.UnexpectedDestination(city) =>
    BadRequest(s"Unexpected city $city")
  }
}
