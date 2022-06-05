package com.trivadis.kafkaws.springbootkotlinkafkaproducer

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.TopicBuilder
import org.springframework.stereotype.Component

@Component
class TopicCreator {

    @Value("\${topic.name}")
    private lateinit var kafkaTopic: String

    @Value("\${topic.partitions}")
    private var partitions: Int = 0

    @Value("\${topic.replication-factor}")
    private var replicationFactor: Int = 0

    @Bean
    fun topic(): NewTopic? {
        return TopicBuilder.name(kafkaTopic)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build()
    }
}