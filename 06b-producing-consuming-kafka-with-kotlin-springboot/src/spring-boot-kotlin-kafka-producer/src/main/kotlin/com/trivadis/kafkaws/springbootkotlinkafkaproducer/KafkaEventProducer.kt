package com.trivadis.kafkaws.springbootkotlinkafkaproducer

import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

@Component
class KafkaEventProducer (private val kafkaTemplate: KafkaTemplate<Long, String>) {

    @Value("\${topic.name}")
    private lateinit var kafkaTopic: String

    fun produce(key: Long, value: String) {
        val time = System.currentTimeMillis()
        var result: SendResult<Long, String>

        if (key > 0) {
            result = kafkaTemplate.send(kafkaTopic, key, value).get()
        } else {
            result = kafkaTemplate.send(kafkaTopic, value).get()
        }

        val elapsedTime = System.currentTimeMillis() - time
        println("[$key] sent record(key=$key value=$value) meta(partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()}) time=$elapsedTime")
    }
}