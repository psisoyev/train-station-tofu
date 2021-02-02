import sbt._

object Dependencies {
  val zioCore = "dev.zio" %% "zio" % Version.zio
  val zioCats = ("dev.zio" %% "zio-interop-cats" % Version.zioCats).excludeAll(ExclusionRule("dev.zio"))
  val zio = List(zioCore, zioCats)

  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % Version.cats

  val zioTest = List(
    "dev.zio" %% "zio-test",
    "dev.zio" %% "zio-test-sbt"
  ).map(_ % Version.zio % Test)

  val tofu = List(
    "ru.tinkoff" %% "tofu",
    "ru.tinkoff" %% "tofu-logging",
    "ru.tinkoff" %% "tofu-logging-log4cats",
    "ru.tinkoff" %% "tofu-logging-layout",
    "ru.tinkoff" %% "tofu-zio-core",
    "ru.tinkoff" %% "tofu-zio-logging"
  ).map(_ % Version.tofu)

  val http4s = List(
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-blaze-server"
  ).map(_ % Version.http4s)

  val fs2Core = "co.fs2" %% "fs2-core" % Version.fs2Core

  val newtype = "io.estatico" %% "newtype" % Version.newtype

  val circe = List(
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-parser"
  ).map(_ % Version.circe)

  val neutronCore = "com.chatroulette" %% "neutron-core" % Version.neutron
  val neutronCirce = "com.chatroulette" %% "neutron-circe" % Version.neutron

  val ciris = "is.cir" %% "ciris" % Version.ciris

  val contextApplied = "org.augustjune" %% "context-applied" % Version.contextApplied
  val kindProjector = "org.typelevel" %% "kind-projector" % Version.kindProjector cross CrossVersion.full
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % Version.betterMonadicFor

  val logback = "ch.qos.logback" % "logback-classic" % Version.logback
  val log4cats = "io.chrisdavenport" %% "log4cats-core" % Version.log4cats
}

object Version {
  val cats = "2.3.1"
  val zioCats = "2.2.0.1"
  val zio = "1.0.4-1"
  val fs2Core = "2.4.2"
  val kindProjector = "0.11.3"
  val ciris = "1.2.1"
  val http4s = "1.0-234-d1a2b53"
  val circe = "0.13.0"
  val newtype = "0.4.4"
  val neutron = "0.0.4"
  val contextApplied = "0.1.4"
  val tofu = "0.9.0"
  val betterMonadicFor = "0.3.1"
  val logback = "1.2.3"
  val log4cats = "1.1.1"
}
