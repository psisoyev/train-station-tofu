package com.psisoyev.train.station.arrival

import cats.{ FlatMap, Monad }
import com.psisoyev.train.station.arrival.ArrivalValidator.ArrivalError.UnexpectedTrain
import com.psisoyev.train.station.arrival.ArrivalValidator.ValidatedArrival
import com.psisoyev.train.station.arrival.Arrivals.Arrival
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ Actual, TrainId }
import derevo.derive
import derevo.tagless.applyK
import io.chrisdavenport.log4cats.Logger
import tofu.Raise
import tofu.higherKind.Mid
import tofu.syntax.monadic._
import tofu.syntax.raise._

import scala.util.control.NoStackTrace

@derive(applyK)
trait ArrivalValidator[F[_]] {
  def validate(arrival: Arrival): F[ValidatedArrival]
}

object ArrivalValidator {
  sealed trait ArrivalError extends NoStackTrace
  object ArrivalError {
    case class UnexpectedTrain(id: TrainId) extends ArrivalError
  }

  case class ValidatedArrival(trainId: TrainId, time: Actual, expectedTrain: ExpectedTrain)

  private class Log[F[_]: FlatMap: Logger] extends ArrivalValidator[Mid[F, *]] {
    def validate(arrival: Arrival): Mid[F, ValidatedArrival] = { validation =>
      F.info(s"Validating $arrival") *> validation <* F.info(s"Train ${arrival.trainId} validated")
    }
  }

  private class Impl[F[_]: Monad: Raise[*[_], ArrivalError]](expectedTrains: ExpectedTrains[F]) extends ArrivalValidator[F] {
    override def validate(arrival: Arrival): F[ValidatedArrival] =
      expectedTrains
        .get(arrival.trainId)
        .flatMap { train =>
          train
            .map(ValidatedArrival(arrival.trainId, arrival.time, _))
            .orRaise(UnexpectedTrain(arrival.trainId))
        }
  }

  def make[F[_]: Monad: Logger: Raise[*[_], ArrivalError]](
    expectedTrains: ExpectedTrains[F]
  ): ArrivalValidator[F] = {
    val service = new Impl[F](expectedTrains)

    (new Log[F]).attach(service)
  }
}
