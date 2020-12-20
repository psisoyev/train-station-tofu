package com.psisoyev.train.station

import cats.Applicative
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cr.pulsar.{ MessageKey, Producer }
import io.chrisdavenport.log4cats.Logger
import org.apache.pulsar.client.api.MessageId
import tofu.generate.GenUUID
import zio.Task
import zio.test.DefaultRunnableSpec

import java.util.UUID

trait BaseSpec extends DefaultRunnableSpec {
  type F[A]           = Task[A]
  type ExpectedTrains = Map[TrainId, ExpectedTrain]

  def fakeProducer[F[_]: Sync]: F[(Ref[F, List[Event]], Producer[F, Event])] =
    Ref.of[F, List[Event]](List.empty).map { ref =>
      ref -> new Producer[F, Event] {
        override def send(msg: Event): F[MessageId]                  = ref.update(_ :+ msg).as(MessageId.latest)
        override def send_(msg: Event): F[Unit]                      = send(msg).void
        override def send(msg: Event, key: MessageKey): F[MessageId] = ???
        override def send_(msg: Event, key: MessageKey): F[Unit]     = ???
      }
    }

  implicit def emptyLogger: Logger[Task] = new Logger[Task] {
    override def error(message: => String): Task[Unit]               = Task.unit
    override def warn(message: => String): Task[Unit]                = Task.unit
    override def info(message: => String): Task[Unit]                = Task.unit
    override def debug(message: => String): Task[Unit]               = Task.unit
    override def trace(message: => String): Task[Unit]               = Task.unit
    override def error(t: Throwable)(message: => String): Task[Unit] = Task.unit
    override def warn(t: Throwable)(message: => String): Task[Unit]  = Task.unit
    override def info(t: Throwable)(message: => String): Task[Unit]  = Task.unit
    override def debug(t: Throwable)(message: => String): Task[Unit] = Task.unit
    override def trace(t: Throwable)(message: => String): Task[Unit] = Task.unit
  }

  val fakeUuid: UUID                                      = UUID.randomUUID()
  val eventId: EventId                                    = EventId(fakeUuid)
  implicit def fakeUuidGen[F[_]: Applicative]: GenUUID[F] = new GenUUID[F] {
    override def randomUUID: F[UUID] = F.pure(fakeUuid)
  }
  implicit def fakeTracing                                = new Tracing[F] {
    override def traced[A](opName: String)(fa: F[A]): F[A] = fa
  }
}
