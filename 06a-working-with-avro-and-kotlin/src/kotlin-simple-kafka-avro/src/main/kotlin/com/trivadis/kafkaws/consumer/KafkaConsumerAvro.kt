@file:JvmName("KafkaConsumerAvro")
package com.trivadis.kafkaws.consumer

import com.trivadis.kafkaws.avro.v1.Notification
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*


private val TOPIC = "test-kotlin-avro-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"
private val SCHEMA_REGISTRY_URL = "http://dataplatform:8081"

fun runConsumerManual(waitMsInBetween: Long) {
    // Define properties.
    val props = Properties()
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[GROUP_ID_CONFIG] = "kotlin-simple-avro-consumer"
    props[ENABLE_AUTO_COMMIT_CONFIG] = false
    props[KEY_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.qualifiedName
    props[VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.qualifiedName
    props[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = SCHEMA_REGISTRY_URL

    val consumer = KafkaConsumer<Long, Notification>(props).apply {
        subscribe(listOf(TOPIC))
    }

    consumer.use {
        while(true) {
            val messages = consumer.poll(Duration.ofMillis(100))

            messages.forEach {
                println("Consumed record [Key: ${it.key()}, Value: ${it.value()}] @ Partition: ${it.partition()}, Offset:  ${it.offset()}")
            }

            consumer.commitSync();

            // Simulate slow processing
            Thread.sleep(waitMsInBetween);
        }
    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        runConsumerManual(10)
    } else {
        runConsumerManual(args[0].toLong())
    }
}
