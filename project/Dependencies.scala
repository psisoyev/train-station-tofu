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
    "ru.tinkoff" %% "tofu-logging"
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

  val slf4j = "org.slf4j" % "slf4j-simple" % Version.slf4j

  val neutronCore = "com.chatroulette" %% "neutron-core" % Version.neutron
  val neutronCirce = "com.chatroulette" %% "neutron-circe" % Version.neutron

  val ciris = "is.cir" %% "ciris" % Version.ciris

  val contextApplied = "org.augustjune" %% "context-applied" % Version.contextApplied
  val kindProjector = "org.typelevel" %% "kind-projector" % Version.kindProjector cross CrossVersion.full
}

object Version {
  val cats = "2.2.0"
  val zio = "1.0.1"
  val zioCats = "2.1.4.0"
  val slf4j = "1.7.28"
  val fs2Core = "2.4.2"
  val kindProjector = "0.11.0"
  val http4s = "1.0.0-M6"
  val ciris = "1.2.0"
  val circe = "0.13.0"
  val newtype = "0.4.4"
  val neutron = "0.0.3"
  val contextApplied = "0.1.3"
  val tofu = "0.8.0"
}
