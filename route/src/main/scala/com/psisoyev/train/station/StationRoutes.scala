package com.psisoyev.train.station

import cats.{ Defer, FlatMap, Monad }
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
import tofu.generate.GenUUID
import tofu.{ Handle, WithRun }
import tofu.syntax.context._

class StationRoutes[I[_]: Monad: Defer: JsonDecoder: GenUUID, F[_]: FlatMap: WithRun[*[_], I, Ctx]](
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
        .asJsonDecode[Arrival]
        .flatMap { arrival =>
          traced(arrivalValidator.validate(arrival).flatMap(arrivals.register)) *> Ok()
        }

      E1.handleWith(res)(handleArrivalErrors)
    case req @ POST -> Root / "departure" =>
      val res = req
        .asJsonDecode[Departure]
        .flatMap(departure => traced(departures.register(departure))) *> Ok()

      E2.handleWith(res)(handleDepartureErrors)
  }

  def traced[T](action: F[T]): I[T] =
    I.randomUUID.map(id => TraceId(id.toString)).flatMap { traceId =>
      runContext(action)(Ctx(traceId))
    }

  def handleArrivalErrors: ArrivalError => I[Response[I]] = { case ArrivalError.UnexpectedTrain(id) =>
    BadRequest(s"Unexpected train $id")
  }

  def handleDepartureErrors: DepartureError => I[Response[I]] = { case DepartureError.UnexpectedDestination(city) =>
    BadRequest(s"Unexpected city $city")
  }
}
