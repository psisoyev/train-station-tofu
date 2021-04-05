package com.psisoyev.train.station

import cats.Show
import cats.implicits.toShow
import cr.pulsar.Topic
import io.circe.Encoder
import io.circe.syntax._
import org.typelevel.log4cats.StructuredLogger

object EventLogger {

  sealed trait EventFlow
  object EventFlow {
    case object In  extends EventFlow
    case object Out extends EventFlow

    implicit def show: Show[EventFlow] = Show.fromToString
  }

  def logEvents[F[_]: StructuredLogger, E: Encoder](flow: EventFlow): E => Topic.URL => F[Unit] =
    event => topic => F.info(Map("topic" -> topic.value, "flow" -> flow.show))(event.asJson.noSpaces)

}
