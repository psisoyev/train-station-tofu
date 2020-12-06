package com.psisoyev.train.station.arrival

import com.psisoyev.train.station.Event.Arrived
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.arrival.ArrivalValidator.ValidatedArrival
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ BaseSpec, To }
import zio.Ref
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object ArrivalsSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("ArrivalsSpec")(
      testM("Register expected train") {
        checkM(trainId, from, city, expected, actual) { (trainId, from, city, expected, actual) =>
          val expectedTrain  = ExpectedTrain(from, expected)
          val expectedTrains = Map(trainId -> expectedTrain)

          for {
            ref               <- Ref.make(expectedTrains)
            expectedTrains     = ExpectedTrains.make[F](ref)
            arrivals           = Arrivals.make[F](city, expectedTrains)
            result            <- arrivals.register(ValidatedArrival(trainId, actual, expectedTrain))
            expectedTrainsMap <- ref.get
          } yield {
            val arrived = Arrived(eventId, trainId, from, To(city), expected, actual.toTimestamp)

            assert(result)(equalTo(arrived)) &&
            assert(expectedTrainsMap.isEmpty)(isTrue)
          }
        }
      }
    )
}
