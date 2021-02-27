package com.psisoyev.train

import derevo.cats.eqv
import derevo.circe.{ decoder, encoder }
import io.estatico.newtype.macros.newtype
import derevo.derive

import java.time.Instant
import java.util.UUID

package object station {
  @derive(decoder, encoder)
  @newtype case class Actual(value: Instant) {
    def toTimestamp: Timestamp = Timestamp(value)
  }

  @derive(decoder, encoder)
  @newtype case class Expected(value: Instant)

  @derive(decoder, encoder)
  @newtype case class Timestamp(value: Instant)

  @derive(decoder, encoder)
  @newtype case class EventId(value: String)
  object EventId {
    def apply(uuid: UUID): EventId = EventId(uuid.toString)
  }

  @derive(decoder, encoder)
  @newtype case class TrainId(value: String)

  @derive(decoder, encoder, eqv)
  @newtype case class City(value: String)

  @derive(decoder, encoder)
  @newtype case class From(city: City)

  @derive(decoder, encoder)
  @newtype case class To(city: City)
}
