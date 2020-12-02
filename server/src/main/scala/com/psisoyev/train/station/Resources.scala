package com.psisoyev.train.station

import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, ContextShift, Resource }
import cats.implicits._
import cats.{ Inject, Parallel }
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cr.pulsar.{ Consumer, Producer, Pulsar, Subscription, Topic, Config => PulsarConfig }
import io.circe.Encoder
import tofu.lift.Lift
import tofu.logging.{ Logging, Logs }
import tofu.syntax.lift._

final case class Resources[F[_], E](
  config: Config,
  producer: Producer[F, E],
  consumers: List[Consumer[F, E]],
  logger: Logging[F]
)

object Resources {
  def make[
    I[_]: Concurrent: ContextShift,
    F[_]: Concurrent: ContextShift: Parallel,
    E: Inject[*, Array[Byte]]: Encoder
  ]: Resource[I, Resources[F, E]] = {
    def topic(config: PulsarConfig, city: City) =
      Topic
        .Builder
        .withName(Topic.Name(city.value.toLowerCase))
        .withConfig(config)
        .withType(Topic.Type.Persistent)
        .build

    def consumer(client: Pulsar.T, config: Config, city: City): Resource[I, Consumer[F, E]] = {
      val name         = s"${city.value}-${config.city.value}"
      val subscription =
        Subscription
          .Builder
          .withName(Subscription.Name(name))
          .withType(Subscription.Type.Failover)
          .build

      Consumer.create[I, F, E](client, topic(config.pulsar, city), subscription)
    }

    def producer(client: Pulsar.T, config: Config): Resource[I, Producer[F, E]] =
      Producer.create[I, F, E](client, topic(config.pulsar, config.city))

    val logs: Logs[I, F] = Logs.sync[I, F]

    for {
      config    <- Resource.liftF(Config.load[I])
      client    <- Pulsar.create[I](config.pulsar.url)
      global    <- Resource.liftF(logs.byName("global"))
      producer  <- producer(client, config)
      consumers <- config.connectedTo.traverse(consumer(client, config, _))
    } yield Resources(config, producer, consumers, global)
  }
}
