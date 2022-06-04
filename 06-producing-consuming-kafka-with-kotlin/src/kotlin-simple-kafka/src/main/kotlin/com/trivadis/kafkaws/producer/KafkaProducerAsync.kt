@file:JvmName("KafkaProducerAsync")
package com.trivadis.kafkaws.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.LocalDateTime
import java.util.*

private val TOPIC = "test-kotlin-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"

fun runProducerAsync(sendMessageCount: Int, waitMsInBetween: Int, id: Long) {
    // Load properties from file
    val props = Properties()

    // Add additional properties.
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[ACKS_CONFIG] = "all"
    props[KEY_SERIALIZER_CLASS_CONFIG] = LongSerializer::class.qualifiedName
    props[VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName

    val key = if (id > 0) id else null

    KafkaProducer<Long, String>(props).use { producer ->
        repeat(sendMessageCount) { index ->
            val time = System.currentTimeMillis();

            val value = "[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now()

            producer.send(ProducerRecord(TOPIC, key, value)) { m: RecordMetadata, e: Exception? ->
                when (e) {
                    // no exception, good to go!
                    null -> {
                        val elapsedTime = System.currentTimeMillis() - time;
                        println("Produced record to topic ${m.topic()} partition [${m.partition()}] @ offset ${m.offset()} time=${elapsedTime}")
                    }
                    // print stacktrace in case of exception
                    else -> e.printStackTrace()
                }
            }
        }

        producer.flush()

    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        runProducerAsync(100, 10, 0)
    } else {
        runProducerAsync(args[0].toInt(), args[1].toInt(), args[2].toLong())
    }
}
