package com.psisoyev.train.station

import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import cats.effect.concurrent.Ref
import com.psisoyev.train.station.arrival.ExpectedTrains
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.departure.DepartureTracker
import cr.pulsar.schema.circe.circeBytesInject
import io.chrisdavenport.log4cats.StructuredLogger
import org.http4s.{ Request, Response }
import tofu.logging.Logging
import tofu.logging.log4cats._
import tofu.logging.zlogs.ZLogs
import tofu.zioInstances.implicits._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{ Ref => _, _ }

object Main extends zio.App {
  type Init[T]      = Task[T]
  type Run[T]       = ZIO[Context, Throwable, T]
  type Routes[F[_]] = Kleisli[F, Request[F], Response[F]]

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    Task.concurrentEffectWith { implicit CE =>
      for {
        global <- ZLogs.withContext[Context].byName("global").map(_.widen[Run])
        pulsar <- ZLogs.uio.byName("pulsar").map(_.widen[Init])
        _      <- startApp(global, pulsar)
      } yield ()
    }.exitCode

  def startApp(
    ctxLogger: Logging[Run],
    eventLogger: Logging[Init]
  )(implicit CE: ConcurrentEffect[Init]): Init[Unit] = {
    implicit val runLogger: StructuredLogger[Run]   = toLog4CatsLogger(ctxLogger)
    implicit val initLogger: StructuredLogger[Init] = toLog4CatsLogger(eventLogger)
    implicit val tracing: Tracing[Run]              = Tracing.make[Run]

    Resources
      .make[Init, Run, Event]
      .use { case Resources(config, producer, consumers) =>
        for {
          trainRef      <- Ref.in[Init, Run, Map[TrainId, ExpectedTrain]](Map.empty)
          expectedTrains = ExpectedTrains.make[Run](trainRef)
          tracker        = DepartureTracker.make[Run](config.city, expectedTrains)
          routes         = Routes.make[Init, Run](config, producer, expectedTrains)

          startHttpServer       = HttpServer.start(config, routes)
          startDepartureTracker = TrackerEngine.start(consumers, tracker)

          _ <- startHttpServer.zipPar(startDepartureTracker)
        } yield ()
      }
  }
}
