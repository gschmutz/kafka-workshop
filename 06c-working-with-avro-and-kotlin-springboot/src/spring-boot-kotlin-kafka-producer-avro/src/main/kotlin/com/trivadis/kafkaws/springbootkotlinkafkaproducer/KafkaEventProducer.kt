package com.trivadis.kafkaws.springbootkotlinkafkaproducer

import com.trivadis.kafkaws.avro.v1.Notification
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

@Component
class KafkaEventProducer (private val kafkaTemplate: KafkaTemplate<Long, Notification>) {

    @Value("\${topic.name}")
    private lateinit var kafkaTopic: String

    fun produce(key: Long, value: Notification) {
        val time = System.currentTimeMillis()
        var result: SendResult<Long, Notification>

        if (key > 0) {
            result = kafkaTemplate.send(kafkaTopic, key, value).get()
        } else {
            result = kafkaTemplate.send(kafkaTopic, value).get()
        }

        val elapsedTime = System.currentTimeMillis() - time
        println("[$key] sent record(key=$key value=$value) meta(partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()}) time=$elapsedTime")
    }
}