package com.psisoyev.train.station

import java.util.UUID

import cats.Applicative
import cats.implicits._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cr.pulsar.Producer
import org.apache.pulsar.client.api.MessageId
import tofu.generate.GenUUID
import tofu.logging.{ Logging, Logs }
import zio.Task
import zio.test.DefaultRunnableSpec
import zio.interop.catz._

trait BaseSpec extends DefaultRunnableSpec {
  type F[A]           = Task[A]
  type ExpectedTrains = Map[TrainId, ExpectedTrain]

  def fakeProducer[F[_]: Sync]: F[(Ref[F, List[Event]], Producer[F, Event])] =
    Ref.of[F, List[Event]](List.empty).map { ref =>
      ref -> new Producer[F, Event] {
        override def send(msg: Event): F[MessageId] = ref.update(_ :+ msg).as(MessageId.latest)
        override def send_(msg: Event): F[Unit]     = send(msg).void
      }
    }

  implicit def emptyLogger: Logging[Task] =
    zio
      .Runtime
      .default
      .unsafeRun(
        Logs.empty[Task, Task].byName("test")
      )

  val fakeUuid: UUID                                      = UUID.randomUUID()
  val eventId: EventId                                    = EventId(fakeUuid)
  implicit def fakeUuidGen[F[_]: Applicative]: GenUUID[F] = new GenUUID[F] {
    override def randomUUID: F[UUID] = F.pure(fakeUuid)
  }
}
