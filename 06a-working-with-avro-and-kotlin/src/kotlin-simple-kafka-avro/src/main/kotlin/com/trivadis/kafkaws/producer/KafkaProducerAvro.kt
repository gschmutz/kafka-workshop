@file:JvmName("KafkaProducerAvro")

package com.trivadis.kafkaws.producer

import com.trivadis.kafkaws.avro.v1.Notification
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import java.time.LocalDateTime
import java.util.*

private val TOPIC = "test-kotlin-avro-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"
private val SCHEMA_REGISTRY_URL = "http://dataplatform:8081"

fun runProducer(sendMessageCount: Int, waitMsInBetween: Long, id: Long) {
    // Define properties.
    val props = Properties()
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[ACKS_CONFIG] = "all"
    props[KEY_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.qualifiedName
    props[VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.qualifiedName
    props[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = SCHEMA_REGISTRY_URL

    val key = if (id > 0) id else null

    KafkaProducer<Long, Notification>(props).use { producer ->
        repeat(sendMessageCount) { index ->
            val time = System.currentTimeMillis();

            //val notification = Notification(id, "test")
            val notification = Notification.newBuilder().setId(id)
                .setMessage("[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now()).build()

            val m = producer.send(ProducerRecord(TOPIC, key, notification)).get()

            val elapsedTime = System.currentTimeMillis() - time;
            println("Produced record to topic ${m.topic()} partition [${m.partition()}] @ offset ${m.offset()} time=${elapsedTime}")

            // Simulate slow processing
            Thread.sleep(waitMsInBetween);
        }

        producer.flush()
    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        runProducer(100, 10, 0)
    } else {
        runProducer(args[0].toInt(), args[1].toLong(), args[2].toLong())
    }
}
