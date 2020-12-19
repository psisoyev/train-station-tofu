package com.psisoyev.train.station

import cr.pulsar.Topic
import io.circe.Encoder
import io.circe.syntax._
import tofu.logging.Logging

object EventLogger {

  def logEvents[F[_]: Logging, E: Encoder]: E => Topic.URL => F[Unit] =
    event => topic => F.info(s"[$topic] ${event.asJson.noSpaces}")

}
