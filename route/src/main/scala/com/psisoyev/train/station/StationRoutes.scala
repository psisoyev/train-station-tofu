package com.psisoyev.train.station

import cats.implicits._
import cats.{ Defer, FlatMap, Monad }
import com.psisoyev.train.station.arrival.ArrivalValidator.ArrivalError
import com.psisoyev.train.station.arrival.Arrivals.Arrival
import com.psisoyev.train.station.arrival.{ ArrivalValidator, Arrivals }
import com.psisoyev.train.station.departure.Departures
import com.psisoyev.train.station.departure.Departures.{ Departure, DepartureError }
import com.psisoyev.train.station.Context.{ withUserContext, UserId }
import cr.pulsar.Producer
import io.circe.Decoder
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, _ }
import tofu.generate.GenUUID
import tofu.{ Handle, HasProvide }

class StationRoutes[
  I[_]: Monad: Defer: JsonDecoder: GenUUID,
  F[_]: FlatMap: HasProvide[*[_], I, Context]
](
  arrivals: Arrivals[F],
  arrivalValidator: ArrivalValidator[F],
  producer: Producer[I, Event],
  departures: Departures[F]
)(implicit
  E1: Handle[I, ArrivalError],
  E2: Handle[I, DepartureError]
) extends Http4sDsl[I] {
  val routes: HttpRoutes[I] = HttpRoutes.of[I] {
    case req @ POST -> Root / "arrival"   =>
      val register = (a: Arrival) => arrivalValidator.validate(a).flatMap(arrivals.register)
      val res      = authorizedRegistration(req)(register)
      E1.handleWith(res)(handleArrivalErrors)
    case req @ POST -> Root / "departure" =>
      val res = authorizedRegistration(req)(departures.register)
      E2.handleWith(res)(handleDepartureErrors)
  }

  def authorizedRegistration[T: Decoder, E <: Event](req: Request[I])(register: T => F[E]): I[Response[I]] =
    for {
      // For simplicity UserId is randomly generated.
      // Normally, it would be taken from request
      userId <- I.randomUUID.map(id => UserId(id.toString))
      action <- req.asJsonDecode[T]
      event  <- withUserContext(userId)(register(action))
      res    <- producer.send_(event) *> Ok()
    } yield res

  def handleArrivalErrors: ArrivalError => I[Response[I]] = { case ArrivalError.UnexpectedTrain(id) =>
    BadRequest(s"Unexpected train $id")
  }

  def handleDepartureErrors: DepartureError => I[Response[I]] = { case DepartureError.UnexpectedDestination(city) =>
    BadRequest(s"Unexpected city $city")
  }
}
