import sbt.fullRunTask

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "scala-kafka",
    resolvers += "Confluent Repo" at "https://packages.confluent.io/maven",
    libraryDependencies ++= (Dependencies.rootDependencies ++ Dependencies.kafkaClientsDeps),
    libraryDependencies ++= (Dependencies.testDependencies map(_ % Test)),
    fullRunTask(produce, Compile, s"com.trivadis.kafkaws.producer.Producer")
  )

lazy val produce: TaskKey[Unit] = taskKey[Unit]("Message Production")
lazy val consume: TaskKey[Unit] = taskKey[Unit]("Message Consumption")
