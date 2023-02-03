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