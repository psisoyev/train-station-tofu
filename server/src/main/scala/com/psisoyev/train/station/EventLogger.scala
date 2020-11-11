package com.psisoyev.train.station

import cr.pulsar.Topic
import io.circe.Encoder
import io.circe.syntax._
import tofu.logging.Logging

object EventLogger {

  private def logEvents[F[_]: Logging, E: Encoder](
    prefix: String
  ): E => Topic.URL => F[Unit] =
    event => topic => F.info(s"[$prefix] [$topic] ${event.asJson.noSpaces}")

  def incomingEvents[F[_]: Logging, E: Encoder]: E => Topic.URL => F[Unit] =
    logEvents[F, E]("Incoming")

  def outgoingEvents[F[_]: Logging, E: Encoder]: E => Topic.URL => F[Unit] =
    logEvents[F, E]("Outgoing")

}
