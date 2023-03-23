package com.trivadis.kafkaws.springbootkafkaconsumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KafkaEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Autowired
    private KafkaTemplate<Long, String> kafkaTemplate;

    @Value("${topic.out.name}")
    private String outputTopic;

    @Value("${force.error.after}")
    private Integer forceErrorAfter;

    private Integer count = 0;

    @KafkaListener(id = "spring-boot-tx", topics = "${topic.in.name}", groupId = "simple-consumer-group", concurrency = "3")
    @Transactional
    public void receive(ConsumerRecord<Long, String> consumerRecord) throws NoSuchMethodException {
        String value = consumerRecord.value();
        Long key = consumerRecord.key();
        LOGGER.info("received key = '{}' with payload='{}'", key, value);

        kafkaTemplate.send(outputTopic, key, value);

        // force failure?
        if (forceErrorAfter != -1 && count > forceErrorAfter) {
            // throwing an NoSuchMethodException to force stopping the KafkaListener (so that no retries are done)
            throw new NoSuchMethodException();
        }

        count++;
    }

}


