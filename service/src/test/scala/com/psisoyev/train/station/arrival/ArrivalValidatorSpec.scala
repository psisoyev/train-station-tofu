package com.psisoyev.train.station.arrival

import cats.effect.concurrent.Ref
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.arrival.ArrivalValidator.ArrivalError.UnexpectedTrain
import com.psisoyev.train.station.arrival.ArrivalValidator.ValidatedArrival
import com.psisoyev.train.station.arrival.Arrivals.Arrival
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ BaseSpec, TrainId }
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object ArrivalValidatorSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("ArrivalValidatorSpec")(
      testM("Validate arriving train") {
        checkM(trainId, from, expected, actual) { (trainId, from, expected, actual) =>
          val expectedTrains = Map(trainId -> ExpectedTrain(from, expected))

          for {
            ref           <- Ref.of[F, Map[TrainId, ExpectedTrain]](expectedTrains)
            expectedTrains = ExpectedTrains.make[F](ref)
            validator      = ArrivalValidator.make[F](expectedTrains)
            result        <- validator.validate(Arrival(trainId, actual))
          } yield {
            val validated = ValidatedArrival(trainId, actual, ExpectedTrain(from, expected))
            assert(result)(equalTo(validated))
          }
        }
      },
      testM("Reject unexpected train") {
        checkM(trainId, actual) { (trainId, actual) =>
          for {
            ref           <- Ref.of[F, Map[TrainId, ExpectedTrain]](Map.empty)
            expectedTrains = ExpectedTrains.make[F](ref)
            validator      = ArrivalValidator.make[F](expectedTrains)
            result        <- validator.validate(Arrival(trainId, actual)).flip
          } yield assert(result)(equalTo(UnexpectedTrain(trainId)))
        }
      }
    )
}
