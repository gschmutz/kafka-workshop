package com.trivadis.kafkaws.springbootkotlinkafkaproducer

import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import org.springframework.util.concurrent.ListenableFuture

@Component
class KafkaEventProducerAsync(private val kafkaTemplate: KafkaTemplate<Long, String>) {

    @Value("\${topic.name}")
    private lateinit var kafkaTopic: String

    fun produce(key: Long, value: String) {
        val time = System.currentTimeMillis()
        var future: ListenableFuture<SendResult<Long, String>>

        if (key > 0) {
            future = kafkaTemplate.send(kafkaTopic, key, value)
        } else {
            future = kafkaTemplate.send(kafkaTopic, value)
        }

        future.addCallback({
            val elapsedTime = System.currentTimeMillis() - time
            println("[$key] sent record(key=$key value=$value) meta(partition=${it?.recordMetadata?.partition()}, offset=${it?.recordMetadata?.offset()}) time=$elapsedTime")
        }, {
            it.printStackTrace()
        })
    }
}