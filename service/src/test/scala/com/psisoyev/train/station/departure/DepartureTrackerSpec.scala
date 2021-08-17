package com.psisoyev.train.station.departure

import cats.implicits._
import com.psisoyev.train.station.Generators._
import com.psisoyev.train.station.arrival.ExpectedTrains
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ BaseSpec, TrainId }
import zio.interop.catz._
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test._
import cats.effect.Ref

object DepartureTrackerSpec extends BaseSpec {
  override def spec: ZSpec[TestEnvironment, Failure] =
    suite("DepartureTrackerSpec")(
      testM("Expect trains departing to $city") {
        checkM(departedList, city) { (departed, city) =>
          for {
            ref           <- Ref.of[F, Map[TrainId, ExpectedTrain]](Map.empty)
            expectedTrains = ExpectedTrains.make[F](ref)
            tracker        = DepartureTracker.make[F](city, expectedTrains)
            _             <- departed.traverse(tracker.save)
            result        <- ref.get
          } yield assert(result.size)(equalTo(departed.count(_.to.city === city)))
        }
      }
    )
}
