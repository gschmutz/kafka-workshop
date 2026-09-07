package com.trivadis.kafkaws.orders;

import com.example.orders.avro.OrderCreated;
import com.trivadis.kafkaws.events.CloudEventHeaders;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
public class OrderEventListener {

    @KafkaListener(
        topics     = "${app.kafka.topics.orders}",
        groupId    = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(ConsumerRecord<String, OrderCreated> record) {

        // --- CE metadata from headers ---
        String ceSpecVersion    = header(record, CloudEventHeaders.SPEC_VERSION);
        String ceId             = header(record, CloudEventHeaders.ID);
        String ceType           = header(record, CloudEventHeaders.TYPE);
        String ceSource         = header(record, CloudEventHeaders.SOURCE);
        String ceTime           = header(record, CloudEventHeaders.TIME);
        String ceDataSchema     = header(record, CloudEventHeaders.DATA_SCHEMA);
        String ceSubject        = header(record, CloudEventHeaders.SUBJECT);
        String cePartitionKey   = header(record, CloudEventHeaders.PARTITION_KEY);
        String contentType      = header(record, CloudEventHeaders.CONTENT_TYPE);

        // --- Business payload from Avro-deserialized value ---
        OrderCreated order = record.value();

        log.info("""
            CloudEvent received:
              ce_specversion    = {}
              ce_id             = {}
              ce_type           = {}
              ce_source         = {}
              ce_time           = {}
              ce_dataschema     = {}
              ce_subject        = {}
              ce_partitionkey   = {}
              content-type      = {}
              orderId           = {}
              customer          = {}
              amount            = {} {}
            """,
            ceSpecVersion, ceId, ceType, ceSource, ceTime, ceDataSchema, ceSubject, cePartitionKey, contentType,
            order.getOrderId(), order.getCustomerId(),
            order.getAmount(), order.getCurrency()
        );

        // business logic here...
    }

    private String header(ConsumerRecord<?, ?> record, String key) {
        return Optional.ofNullable(record.headers().lastHeader(key))
            .map(Header::value)
            .map(v -> new String(v, StandardCharsets.UTF_8))
            .orElse(null);
    }
}