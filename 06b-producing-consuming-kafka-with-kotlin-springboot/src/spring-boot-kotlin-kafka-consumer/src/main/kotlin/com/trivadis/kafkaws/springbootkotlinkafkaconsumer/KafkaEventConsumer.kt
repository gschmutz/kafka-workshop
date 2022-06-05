package com.trivadis.kafkaws.springbootkotlinkafkaconsumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaEventConsumer {
    @KafkaListener(topics = ["\${topic.name}"], groupId = "simple-kotlin-consumer")
    fun receive(consumerRecord: ConsumerRecord<Long, String>) {
        println("received key = ${consumerRecord.key()} with payload=${consumerRecord.value()}")
    }
}