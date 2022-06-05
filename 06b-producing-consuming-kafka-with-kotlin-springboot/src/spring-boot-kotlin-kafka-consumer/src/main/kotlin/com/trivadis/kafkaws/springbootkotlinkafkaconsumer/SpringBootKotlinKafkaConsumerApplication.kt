package com.trivadis.kafkaws.springbootkotlinkafkaconsumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory


@SpringBootApplication
class SpringBootKotlinKafkaConsumerApplication

fun main(args: Array<String>) {
	runApplication<SpringBootKotlinKafkaConsumerApplication>(*args)
}

