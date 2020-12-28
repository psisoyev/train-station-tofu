package com.psisoyev.train.station

import cats.effect.Concurrent
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.departure.DepartureTracker
import cr.pulsar.Consumer
import fs2.Stream
import tofu.HasProvide
import tofu.generate.GenUUID

object TrackerEngine {
  def start[Init[_]: Concurrent: GenUUID, Run[_]: HasProvide[*[_], Init, Context]](
    consumers: List[Consumer[Init, Event]],
    departureTracker: DepartureTracker[Run]
  ): Init[Unit] =
    Stream
      .emits(consumers)
      .map(_.autoSubscribe)
      .parJoinUnbounded
      .collect { case e: Departed => e }
      .evalMap(e => Context.withSystemContext(departureTracker.save(e)))
      .compile
      .drain
}
