package com.psisoyev.train.station.departure

import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.departure.Departures.{ Departure, DepartureError }
import com.psisoyev.train.station.{ BaseSpec, From }
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object DeparturesSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("DeparturesSpec")(
      testM("Fail to register departing train to an unexpected destination city") {
        checkM(trainId, to, city, expected, actual) { (trainId, to, city, expected, actual) =>
          Departures
            .make[F](city, List())
            .register(Departure(trainId, to, expected, actual))
            .flip
            .map(assert(_)(equalTo(DepartureError.UnexpectedDestination(to.city))))
        }
      },
      testM("Register departing train") {
        checkM(trainId, to, city, expected, actual) { (trainId, to, city, expected, actual) =>
          Departures
            .make[F](city, List(to.city))
            .register(Departure(trainId, to, expected, actual))
            .map { result =>
              val departed = Departed(eventId, trainId, From(city), to, expected, actual.toTimestamp)
              assert(result)(equalTo(departed))
            }
        }
      }
    )
}
