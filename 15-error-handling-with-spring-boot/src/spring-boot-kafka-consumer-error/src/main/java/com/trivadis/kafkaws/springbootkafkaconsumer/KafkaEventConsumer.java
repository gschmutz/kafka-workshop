package com.trivadis.kafkaws.springbootkafkaconsumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @RetryableTopic(attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = "${topic.name}", groupId = "simple-consumer-group")
    public void listen(ConsumerRecord<String, String> consumerRecord, @Header(KafkaHeaders.RECEIVED_TOPIC) String topicName) {
        String value = consumerRecord.value();
        String key = consumerRecord.key();

        LOGGER.info("received key = '{}' with payload='{}' from topic='{}'", key, value, topicName);

        // message is invalid, if value starts with @
        if (value.startsWith("@")) {
            throw new RuntimeException("Error in consumer");
        }
        LOGGER.info("message with key = '{}' with payload='{}' from topic='{}' processed successfully!", key, value, topicName);

    }

    @DltHandler
    public void dlt(String in, @Header(KafkaHeaders.RECEIVED_TOPIC) String topicName) {
        LOGGER.info(in + " from " + topicName);
    }
}