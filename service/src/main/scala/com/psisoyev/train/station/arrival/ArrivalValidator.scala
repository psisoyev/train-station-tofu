package com.psisoyev.train.station.arrival

import cats.{ FlatMap, Monad }
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.arrival.ArrivalValidator.ArrivalError.UnexpectedTrain
import com.psisoyev.train.station.arrival.ArrivalValidator.ValidatedArrival
import com.psisoyev.train.station.arrival.Arrivals.Arrival
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.Tracing.ops.TracingOps
import com.psisoyev.train.station.{ Actual, Tracing, TrainId }
import derevo.derive
import derevo.tagless.applyK
import io.chrisdavenport.log4cats.Logger
import tofu.{ Handle, Raise }
import tofu.higherKind.Mid
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.monoid.TofuSemigroupOps

import scala.util.control.NoStackTrace

@derive(applyK)
trait ArrivalValidator[F[_]] {
  def validate(arrival: Arrival): F[ValidatedArrival]
}

object ArrivalValidator {
  sealed trait ArrivalError extends NoStackTrace
  object ArrivalError {
    type Handling[F[_]] = Handle[F, ArrivalError]
    type Raising[F[_]]  = Raise[F, ArrivalError]

    case class UnexpectedTrain(id: TrainId) extends ArrivalError
  }

  case class ValidatedArrival(trainId: TrainId, time: Actual, expectedTrain: ExpectedTrain)

  private class Log[F[_]: FlatMap: Logger] extends ArrivalValidator[Mid[F, *]] {
    def validate(arrival: Arrival): Mid[F, ValidatedArrival] = { validation =>
      F.info(s"Validating $arrival") *> validation <* F.info(s"Train ${arrival.trainId} validated")
    }
  }

  private class Trace[F[_]: Tracing] extends ArrivalValidator[Mid[F, *]] {
    def validate(arrival: Arrival): Mid[F, ValidatedArrival] = _.traced("train arrival: validation")
  }

  private class Impl[F[_]: Monad: ArrivalError.Raising](expectedTrains: ExpectedTrains[F]) extends ArrivalValidator[F] {
    override def validate(arrival: Arrival): F[ValidatedArrival] =
      expectedTrains
        .get(arrival.trainId)
        .flatMap { train =>
          train
            .map(ValidatedArrival(arrival.trainId, arrival.time, _))
            .orRaise(UnexpectedTrain(arrival.trainId))
        }
  }

  def make[F[_]: Monad: Logger: ArrivalError.Raising: Tracing](
    expectedTrains: ExpectedTrains[F]
  ): ArrivalValidator[F] = {
    val service = new Impl[F](expectedTrains)

    val log: ArrivalValidator[Mid[F, *]]   = new Log[F]
    val trace: ArrivalValidator[Mid[F, *]] = new Trace[F]

    (log |+| trace).attach(service)
  }
}
