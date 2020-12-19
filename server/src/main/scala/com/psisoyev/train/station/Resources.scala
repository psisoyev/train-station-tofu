package com.psisoyev.train.station

import cats.effect.{ Concurrent, ContextShift, Resource, Sync }
import cats.implicits._
import cats.{ Inject, Parallel }
import cr.pulsar.{ Consumer, Producer, Pulsar, Subscription, Topic, Config => PulsarConfig }
import io.circe.Encoder
import Context.loggableContext
import tofu.HasContext
import tofu.logging.{ Logging, Logs }

final case class Resources[I[_], F[_], E](
  config: Config,
  producer: Producer[I, E],
  consumers: List[Consumer[I, E]],
  logger: Logging[F]
)

object Resources {
  def make[
    I[_]: Concurrent: ContextShift: Parallel,
    F[_]: Sync: *[_] HasContext Context,
    E: Inject[*, Array[Byte]]: Encoder
  ]: Resource[I, Resources[I, F, E]] = {
    def topic(config: PulsarConfig, city: City) =
      Topic
        .Builder
        .withName(Topic.Name(city.value.toLowerCase))
        .withConfig(config)
        .withType(Topic.Type.Persistent)
        .build

    def consumer(client: Pulsar.T, config: Config, city: City)(implicit L: Logging[I]): Resource[I, Consumer[I, E]] = {
      val name         = s"${city.value}-${config.city.value}"
      val subscription =
        Subscription
          .Builder
          .withName(Subscription.Name(name))
          .withType(Subscription.Type.Failover)
          .build

      Consumer.withLogger[I, E](client, topic(config.pulsar, city), subscription, EventLogger.logEvents)
    }

    def producer(client: Pulsar.T, config: Config)(implicit L: Logging[I]): Resource[I, Producer[I, E]] =
      Producer.withLogger[I, E](client, topic(config.pulsar, config.city), EventLogger.logEvents)

    for {
      config         <- Resource.liftF(Config.load[I])
      client         <- Pulsar.create[I](config.pulsar.url)
      consumerLogger <- Resource.liftF(Logs.sync[I, I].byName("<<<"))
      producerLogger <- Resource.liftF(Logs.sync[I, I].byName(">>>"))
      global         <- Resource.liftF(Logs.withContext[I, F].byName("global"))
      producer       <- producer(client, config)(producerLogger)
      consumers      <- config.connectedTo.traverse(consumer(client, config, _)(consumerLogger))
    } yield Resources[I, F, E](config, producer, consumers, global)
  }
}
