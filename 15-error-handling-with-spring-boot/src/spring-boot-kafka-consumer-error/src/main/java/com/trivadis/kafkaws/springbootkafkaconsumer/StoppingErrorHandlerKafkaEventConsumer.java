package com.trivadis.kafkaws.springbootkafkaconsumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerStoppingErrorHandler;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class StoppingErrorHandlerKafkaEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoppingErrorHandlerKafkaEventConsumer.class);

    private final TaskScheduler scheduler;

    private final KafkaListenerEndpointRegistry registry;

    @Value(value = "${topic.stopping-topic-name}")
    public String stoppingTopicName;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public StoppingErrorHandlerKafkaEventConsumer(TaskScheduler scheduler, KafkaListenerEndpointRegistry registry,
                                                  AbstractKafkaListenerContainerFactory<?, ?, ?> factory) {
        this.scheduler = scheduler;
        this.registry = registry;
        factory.setErrorHandler(new ContainerStoppingErrorHandler());
    }

    @KafkaListener(id = "${topic.stopping-topic-name}.id", topics = "${topic.stopping-topic-name}", groupId = "simple-consumer-group")
    public void listen(ConsumerRecord<String, String> consumerRecord, @Header(KafkaHeaders.RECEIVED_TOPIC) String topicName) {
        String value = consumerRecord.value();
        String key = consumerRecord.key();

        LOGGER.info("received key = '{}' with payload='{}' from topic='{}'", key, value, topicName);

        // restart the container automatically after 60 seconds
        this.scheduler.schedule(() -> {
            this.registry.getListenerContainer(stoppingTopicName + ".id").start();

        }, new Date(System.currentTimeMillis() + 60000));

        // this will cause an error if postgres is not available
        Integer dummy = jdbcTemplate.queryForObject("select 1 as value", Integer.class);

        LOGGER.info("message with key = '{}' with payload='{}' from topic='{}' processed successfully!", key, value, topicName);
    }
}