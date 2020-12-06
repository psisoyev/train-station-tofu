package com.psisoyev.train.station.arrival

import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.{ Expected, From, TrainId }
import tofu.lift.Lift
import zio.{ Ref, UIO }

trait ExpectedTrains[F[_]] {
  def get(id: TrainId): F[Option[ExpectedTrain]]
  def remove(id: TrainId): F[Unit]
  def update(id: TrainId, expectedTrain: ExpectedTrain): F[Unit]
}

object ExpectedTrains {
  case class ExpectedTrain(from: From, time: Expected)

  def make[F[_]: Lift[UIO, *[_]]](
    ref: Ref[Map[TrainId, ExpectedTrain]]
  ): ExpectedTrains[F] = new ExpectedTrains[F] {
    override def get(id: TrainId): F[Option[ExpectedTrain]]         = F.lift(ref.get.map(_.get(id)))
    override def remove(id: TrainId): F[Unit]                       = F.lift(ref.update(_.removed(id)))
    override def update(id: TrainId, train: ExpectedTrain): F[Unit] = F.lift(ref.update(_.updated(id, train)))
  }
}
