package com.psisoyev.train.station

import cats.effect.{ Concurrent, ContextShift, Resource, Sync }
import cats.implicits._
import cats.{ Inject, Parallel }
import com.psisoyev.train.station.Context.WithCtx
import com.psisoyev.train.station.EventLogger.EventFlow
import cr.pulsar.{ Consumer, Producer, Pulsar, Subscription, Topic, Config => PulsarConfig }
import org.typelevel.log4cats.StructuredLogger
import io.circe.Encoder

final case class Resources[I[_], F[_], E](
  config: Config,
  producer: Producer[I, E],
  consumers: List[Consumer[I, E]]
)

object Resources {
  def make[
    I[_]: Concurrent: ContextShift: Parallel: StructuredLogger,
    F[_]: Sync: WithCtx,
    E: Inject[*, Array[Byte]]: Encoder
  ]: Resource[I, Resources[I, F, E]] = {
    def topic(config: PulsarConfig, city: City) =
      Topic
        .Builder
        .withName(Topic.Name(city.value.toLowerCase))
        .withConfig(config)
        .withType(Topic.Type.Persistent)
        .build

    def consumer(client: Pulsar.T, config: Config, city: City): Resource[I, Consumer[I, E]] = {
      val name         = s"${city.value}-${config.city.value}"
      val subscription =
        Subscription
          .Builder
          .withName(Subscription.Name(name))
          .withType(Subscription.Type.Failover)
          .build

      Consumer.withLogger[I, E](client, topic(config.pulsar, city), subscription, EventLogger.logEvents(EventFlow.In))
    }

    def producer(client: Pulsar.T, config: Config): Resource[I, Producer[I, E]] =
      Producer.withLogger[I, E](client, topic(config.pulsar, config.city), EventLogger.logEvents(EventFlow.Out))

    for {
      config    <- Resource.eval(Config.load[I])
      client    <- Pulsar.create[I](config.pulsar.url)
      producer  <- producer(client, config)
      consumers <- config.connectedTo.traverse(consumer(client, config, _))
    } yield Resources[I, F, E](config, producer, consumers)
  }
}
