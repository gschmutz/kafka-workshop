package com.trivadis.kafkaws.springcloudstreamkafkaproduceravro;

import com.trivadis.kafkaws.avro.v1.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventProducer.class);

    @Autowired
    private Processor processor;

    @Value("${topic.name}")
    String kafkaTopic;

    public void produce(Integer id, Long key, Notification notification) {
        long time = System.currentTimeMillis();

        Message<Notification> message = MessageBuilder.withPayload(notification)
                .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .build();

        processor.output()
                .send(message);

        long elapsedTime = System.currentTimeMillis() - time;

        System.out.printf("[" + id + "] sent record(key=%s value=%s) time=%d\n",key, notification,elapsedTime);
    }
}