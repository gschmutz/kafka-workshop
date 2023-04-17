package com.trivadis.kafkaws.springbootkafkaconsumeravro;

import com.trivadis.kafkaws.avro.v1.AlertSentEvent;
import com.trivadis.kafkaws.avro.v1.Notification;
import com.trivadis.kafkaws.avro.v1.NotificationSentEvent;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @KafkaListener(topics = "${topic.name}", groupId = "simple-consumer-group")
    public void receive(ConsumerRecord<Long, SpecificRecord> consumerRecord) {
        SpecificRecord value = consumerRecord.value();
        Long key = consumerRecord.key();

        if (value instanceof AlertSentEvent) {
            LOGGER.info("AlertSentEvent: received key = '{}' with payload='{}'", key, (AlertSentEvent)value);
        } else if (value instanceof NotificationSentEvent) {
            LOGGER.info("NotificationSentEvent: received key = '{}' with payload='{}'", key, (NotificationSentEvent)value);
        } else {
            LOGGER.info("Event not supported by consumer: received key = '{}' with payload='{}'", key, value);
        };

        /*
        if (value instanceof AlertSentEvent alertSentEvent) {
            LOGGER.info("AlertSentEvent: received key = '{}' with payload='{}'", key, alertSentEvent);
        } else if (value instanceof NotificationSentEvent notificationSentEvent) {
            LOGGER.info("NotificationSentEvent: received key = '{}' with payload='{}'", key, notificationSentEvent);
        } else {
            LOGGER.info("Event not supported by consumer: received key = '{}' with payload='{}'", key, value);
        };
        */
    }
}
