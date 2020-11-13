package com.psisoyev.train.station.arrival

import cats.effect.concurrent.Ref
import com.psisoyev.train.station.Event.Arrived
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.arrival.ArrivalValidator.ValidatedArrival
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ BaseSpec, To }
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object ArrivalsSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("ArrivalsSpec")(
      testM("Register expected train") {
        checkM(trainId, from, city, expected, actual) {
          (trainId, from, city, expected, actual) =>
            val expectedTrain  = ExpectedTrain(from, expected)
            val expectedTrains = Map(trainId -> expectedTrain)

            for {
              ref                <- Ref.of[F, ExpectedTrains](expectedTrains)
              expectedTrains     = ExpectedTrains.make[F](ref)
              (events, producer) <- fakeProducer[F]
              arrivals           = Arrivals.make[F](city, producer, expectedTrains)
              result             <- arrivals.register(ValidatedArrival(trainId, actual, expectedTrain))
              newEvents          <- events.get
              expectedTrainsMap  <- ref.get
            } yield {
              val arrived = Arrived(eventId, trainId, from, To(city), expected, actual.toTimestamp)
              assert(result)(equalTo(arrived)) &&
              assert(List(result))(equalTo(newEvents)) &&
              assert(expectedTrainsMap.isEmpty)(isTrue)
            }
        }
      }
    )
}
