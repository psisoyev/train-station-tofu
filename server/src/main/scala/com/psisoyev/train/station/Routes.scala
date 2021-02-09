package com.psisoyev.train.station

import cats.Monad
import cats.effect.Sync
import com.psisoyev.train.station.Context.RunsCtx
import com.psisoyev.train.station.Main.Routes
import com.psisoyev.train.station.arrival.ArrivalValidator.ArrivalError
import com.psisoyev.train.station.arrival.{ ArrivalValidator, Arrivals, ExpectedTrains }
import com.psisoyev.train.station.departure.Departures
import com.psisoyev.train.station.departure.Departures.DepartureError
import cr.pulsar.Producer
import org.http4s.implicits._
import tofu.generate.GenUUID
import tofu.logging.Logging

object Routes {
  def make[
    Init[_]: Sync,
    Run[_]: Monad: GenUUID: RunsCtx[*[_], Init]: Logging: Tracing: DepartureError.Raising: ArrivalError.Raising
  ](
    config: Config,
    producer: Producer[Init, Event],
    expectedTrains: ExpectedTrains[Run]
  ): Routes[Init] = {
    val arrivalValidator = ArrivalValidator.make[Run](expectedTrains)
    val arrivals         = Arrivals.make[Run](config.city, expectedTrains)
    val departures       = Departures.make[Run](config.city, config.connectedTo)

    new StationRoutes[Init, Run](arrivals, arrivalValidator, producer, departures).routes.orNotFound
  }
}
