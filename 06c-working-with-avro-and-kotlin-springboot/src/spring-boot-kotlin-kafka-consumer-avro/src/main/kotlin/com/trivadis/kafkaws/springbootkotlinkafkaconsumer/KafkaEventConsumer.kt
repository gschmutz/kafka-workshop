package com.trivadis.kafkaws.springbootkotlinkafkaconsumer

import com.trivadis.kafkaws.avro.v1.Notification
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaEventConsumer {
    @KafkaListener(topics = ["\${topic.name}"], groupId = "simple-kotlin-avro-consumer")
    fun receive(consumerRecord: ConsumerRecord<Long, Notification>) {
        println("received key = ${consumerRecord.key()} with payload=${consumerRecord.value()}")
    }
}