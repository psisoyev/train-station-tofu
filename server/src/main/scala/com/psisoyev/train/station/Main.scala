package com.psisoyev.train.station

import cats.Parallel
import cats.data.Kleisli
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, ConcurrentEffect, ContextShift, Timer }
import com.psisoyev.train.station.Event.Departed
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import com.psisoyev.train.station.arrival.{ ArrivalValidator, Arrivals, ExpectedTrains }
import com.psisoyev.train.station.departure.{ DepartureTracker, Departures }
import cr.pulsar.schema.circe.circeBytesInject
import cr.pulsar.{ Consumer, Producer }
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{ Request, Response }
import tofu.WithRun
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.zioInstances.implicits._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends zio.App {
  type Init[T]      = Task[T]
  type Run[T]       = ZIO[Ctx, Throwable, T]
  type Routes[F[_]] = Kleisli[F, Request[F], Response[F]]

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    Task.concurrentEffectWith { implicit CE =>
      Resources
        .make[Init, Run, Event]
        .use { case Resources(config, producer, consumers, logger) =>
          Ref
            .of[Run, Map[TrainId, ExpectedTrain]](Map.empty)
            .map(makeApp[Init, Run](config, producer, consumers, logger, _))
        }
        .flatMap { case (config, consumers, departureTracker, routes) =>
          runApp[Init, Run](config, consumers, departureTracker, routes).tupled
        }
        .provide(Ctx(Map.empty)) // TODO how to get rid of this?
    }.exitCode

  def makeApp[
    Init[_]: Concurrent: ContextShift,
    Run[_]: Concurrent: ContextShift: Parallel: WithRun[*[_], Init, Ctx]
  ](
    config: Config,
    producer: Producer[Run, Event],
    consumers: List[Consumer[Run, Event]],
    logger: Logging[Run],
    trainRef: Ref[Run, Map[TrainId, ExpectedTrain]]
  ): (Config, List[Consumer[Run, Event]], DepartureTracker[Run], Routes[Init]) = {
    implicit val logging: Logging[Run] = logger
    implicit val tracing: Tracing[Run] = Tracing.make[Run]

    val expectedTrains   = ExpectedTrains.make[Run](trainRef)
    val arrivalValidator = ArrivalValidator.make[Run](expectedTrains)
    val arrivals         = Arrivals.make[Run](config.city, producer, expectedTrains)
    val departures       = Departures.make[Run](config.city, config.connectedTo, producer)
    val departureTracker = DepartureTracker.make[Run](config.city, expectedTrains)

    val routes = new StationRoutes[Init, Run](arrivals, arrivalValidator, departures).routes.orNotFound

    (config, consumers, departureTracker, routes)
  }

  def runApp[Init[_]: ConcurrentEffect: Timer, Run[_]: Concurrent](
    config: Config,
    consumers: List[Consumer[Run, Event]],
    departureTracker: DepartureTracker[Run],
    routes: Routes[Init]
  ): (Run[Unit], Init[Unit]) = {
    val httpServer: Init[Unit] =
      BlazeServerBuilder[Init](platform.executor.asEC)
        .bindHttp(config.httpPort.value, "0.0.0.0")
        .withHttpApp(routes)
        .serve
        .compile
        .drain

    val departureListener: Run[Unit] =
      Stream
        .emits(consumers)
        .map(_.autoSubscribe)
        .parJoinUnbounded
        .collect { case e: Departed => e }
        .evalMap(departureTracker.save)
        .compile
        .drain

    (departureListener, httpServer)
  }

}
