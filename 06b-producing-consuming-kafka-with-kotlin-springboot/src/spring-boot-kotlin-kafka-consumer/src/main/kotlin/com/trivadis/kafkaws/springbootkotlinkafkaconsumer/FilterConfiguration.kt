package com.trivadis.kafkaws.springbootkotlinkafkaconsumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory

@Configuration
class FilterConfiguration {

    @Autowired
    private lateinit var consumerFactory: ConsumerFactory<Long, String>

    @Bean
    fun filterContainerFactory(): ConcurrentKafkaListenerContainerFactory<Long, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, String>()
        factory.setConsumerFactory(consumerFactory)
        factory.setRecordFilterStrategy { record: ConsumerRecord<Long, String> ->
            !record.value().contains("Kafka 5")
        }
        return factory
    }
}