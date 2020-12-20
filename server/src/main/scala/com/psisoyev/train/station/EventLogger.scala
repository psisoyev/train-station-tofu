package com.psisoyev.train.station

import cr.pulsar.Topic
import io.chrisdavenport.log4cats.StructuredLogger
import io.circe.Encoder
import io.circe.syntax._

object EventLogger {

  def logEvents[F[_]: StructuredLogger, E: Encoder](flow: String): E => Topic.URL => F[Unit] =
    event => topic => F.info(Map("topic" -> topic.value, "flow" -> flow))(event.asJson.noSpaces)

}
