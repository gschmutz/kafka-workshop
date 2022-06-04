@file:JvmName("KafkaConsumerManual")
package com.trivadis.kafkaws.consumer

import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*


private val TOPIC = "test-kotlin-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"

fun runConsumerManual(waitMsInBetween: Int) {
    // Load properties from file
    val props = Properties()

    // Add additional properties.
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[GROUP_ID_CONFIG] = "kotlin-simple-consumer"
    props[ENABLE_AUTO_COMMIT_CONFIG] = false
    props[KEY_DESERIALIZER_CLASS_CONFIG] = LongDeserializer::class.qualifiedName
    props[VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.qualifiedName

    val consumer = KafkaConsumer<String, String>(props).apply {
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
            Thread.sleep(waitMsInBetween.toLong());
        }
    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        runConsumerManual(10)
    } else {
        runConsumerManual(args[0].toInt())
    }
}
