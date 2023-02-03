# Using Kafka from Scala

In this workshop we will learn how to use Apache Kafka from Scala.

## Setup

### Creating the necessary Kafka Topic 

We will use the topic `test-node-topic` in the Producer and Consumer code below. Due to the fact that `auto.topic.create.enable` is set to `false`, we have to manually create the topic. 

Connect to the `kafka-1` container

```
docker exec -ti kafka-1 bash
```

and execute the necessary kafka-topics command. 

```
kafka-topics --create \
    --replication-factor 3 \
    --partitions 12 \
    --topic test-scala-topic \
    --bootstrap-server kafka-1:19092
```

This finishes the setup steps and our new project is ready to be used. Next we will start implementing the **Kafka Producer** which uses Avro for the serialisation. 


## Create a Scala Project

Add the following code to `build.sbt`

```bash
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

```

Add the following code to `Dependencies.scala`

```
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
```

# Create Kafka Producer

Next we are going to create the producer application by pasting the following Scala code into a file named `Producer.scala`.

```scala
package com.trivadis.kafkaws.producer

import java.util.Properties
import org.apache.kafka.clients.producer._

object KafkaProducerSync {

  val bootstrapServers = "dataplatform:9092,dataplatform:9093"
  val topic = "test-scala-topic"

  val props: Properties = {
    val p = new Properties()
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongSerializer")
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    p.put(ProducerConfig.ACKS_CONFIG, "all")
    //    p.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384)
    //    p.put(ProducerConfig.LINGER_MS_CONFIG, 1)
    //    p.put(ProducerConfig.RETRIES_CONFIG, 0)

    p
  }

  def main(args: Array[String]): Unit = {

    val sendMessageCount = args(0).toString.toInt
    val waitMsInBetween = args(1).toString.toInt
    val id = args(2).toString.toLong;

    val producer = new KafkaProducer[Long, String](props)

    for (i <- 1 to sendMessageCount) {
      val metadata = if (id > 0)
        producer.send(new ProducerRecord(topic, id, s"[${i}] Hello Kafka ${i}"))
      else
        producer.send(new ProducerRecord(topic, s"[${i}] Hello Kafka ${i}"))
      println(s"[${id}] sent record(topic=${metadata.get().topic()} partition=${metadata.get().partition()}")

      // Simulate slow processing
      Thread.sleep(waitMsInBetween)
    }

    producer.close()
  }

}
```

# Kafka Consumer

Next let's create the consumer by pasting the following code into a file named `consumer.go`.

```scala
package com.trivadis.kafkaws.consumer

import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{CommitFailedException, ConsumerConfig, KafkaConsumer}

object KafkaConsumerExample {

  import scala.collection.JavaConverters._

  val bootstrapServers = "dataplatform:9092,dataplatform:9093"
  val groupId = "kafka-example"
  val topics = "test-scala-topic"

  val props: Properties = {
    val p = new Properties()
    p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongDeserializer")
    p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") // default is latest

    p
  }

  def main(args: Array[String]): Unit = {

    val consumer = new KafkaConsumer[Long, String](props)

    consumer.subscribe(Collections.singletonList(this.topics))

    while (true) {
      val records = consumer.poll(1000).asScala

      for (record <- records) {
        println("Message: (key: " +
          record.key() + ", with value: " + record.value() +
          ") at on partition " + record.partition() + " at offset " + record.offset())
      }

      try
        consumer.commitSync
      catch {
        case e: CommitFailedException =>
      }
    }

    consumer.close()
  }
}
```
