package com.psisoyev.train.station

import cr.pulsar.Topic
import io.chrisdavenport.log4cats.Logger
import io.circe.Encoder
import io.circe.syntax._

object EventLogger {

  def logEvents[F[_]: Logger, E: Encoder]: E => Topic.URL => F[Unit] =
    event => topic => F.info(s"[$topic] ${event.asJson.noSpaces}")

}
