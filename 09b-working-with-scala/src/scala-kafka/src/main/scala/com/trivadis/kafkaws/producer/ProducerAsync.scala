package com.trivadis.kafkaws.producer

import java.util.Properties
import org.apache.kafka.clients.producer._

object KafkaProducerAsync {
  val callback = new Callback {
    override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
      println(s"sent record(topic=${metadata.topic()} partition=${metadata.partition()}")
    }
  }

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
    val id = args(2).toString.toLong

    val producer = new KafkaProducer[Long, String](props)

    for (i <- 1 to sendMessageCount) {
      val metadata = producer.send(new ProducerRecord(topic, id, s"[${i}] Hello Kafka ${i}"), callback)

      // Simulate slow processing
      Thread.sleep(waitMsInBetween)
    }

    producer.close()
  }

}