import sbt._

object Dependencies {

  lazy val rootDependencies: List[ModuleID] =
    "org.typelevel" %% "cats-core" % "2.1.1" ::
      "ch.qos.logback" % "logback-classic" % "1.4.5" :: Nil

  lazy val kafkaClientsDeps: List[ModuleID] =
    "org.apache.kafka" % "kafka-clients" % "3.3.2" :: Nil

  lazy val testDependencies: List[ModuleID] =
    "org.scalatest" %% "scalatest" % "3.2.3" ::
      "org.scalactic" %% "scalactic" % "3.2.3" ::
      "org.scalacheck" %% "scalacheck" % "1.15.1" ::
      "org.typelevel" %% "cats-core" % "2.3.0" :: Nil
}
