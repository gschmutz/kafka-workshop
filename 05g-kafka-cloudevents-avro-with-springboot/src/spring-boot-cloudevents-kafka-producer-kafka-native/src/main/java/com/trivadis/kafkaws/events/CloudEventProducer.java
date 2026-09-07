package com.trivadis.kafkaws.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${cloudevents.source}")
    private String defaultSource;

    @Value("${cloudevents.schema-registry-base-url}")
    private String schemaRegistryBaseUrl;

    /**
     * Publish an Avro record as a CloudEvent in binary mode.
     *
     * @param topic       target Kafka topic
     * @param key         Kafka message key (typically aggregate ID)
     * @param payload     Avro SpecificRecord (serialized by KafkaAvroSerializer)
     * @param eventType   CE type, e.g. "com.example.orders.OrderCreated"
     * @param subjectName Schema Registry subject for ce_dataschema header
     */
    public <T extends SpecificRecord> CompletableFuture<SendResult<String, Object>> publish(
            String topic,
            String key,
            T payload,
            String eventType,
            String subjectName) {

        RecordHeaders headers = buildCeHeaders(eventType, subjectName);

        ProducerRecord<String, Object> record =
            new ProducerRecord<>(topic, null, key, payload, headers);

        return kafkaTemplate.send(record)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish CE [type={}, key={}]: {}",
                        eventType, key, ex.getMessage(), ex);
                } else {
                    log.info("Published CE [type={}, key={}, partition={}, offset={}]",
                        eventType, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }

    // Overload without subjectName (omits ce_dataschema header)
    public <T extends SpecificRecord> CompletableFuture<SendResult<String, Object>> publish(
            String topic, String key, T payload, String eventType) {
        return publish(topic, key, payload, eventType, null);
    }

    // ---------------------------------------------------------------

    private RecordHeaders buildCeHeaders(String eventType, String subjectName) {
        RecordHeaders headers = new RecordHeaders();
        headers.add(CloudEventHeaders.SPEC_VERSION, utf8(CloudEventHeaders.SPEC_VERSION_1_0));
        headers.add(CloudEventHeaders.ID,           utf8(UUID.randomUUID().toString()));
        headers.add(CloudEventHeaders.TYPE,         utf8(eventType));
        headers.add(CloudEventHeaders.SOURCE,       utf8(defaultSource));
        headers.add(CloudEventHeaders.TIME,         utf8(Instant.now().toString()));
        headers.add(CloudEventHeaders.CONTENT_TYPE, utf8(CloudEventHeaders.AVRO_CONTENT_TYPE));

        if (subjectName != null) {
            String dataSchema = schemaRegistryBaseUrl
                + "/subjects/" + subjectName + "/versions/latest";
            headers.add(CloudEventHeaders.DATA_SCHEMA, utf8(dataSchema));
        }
        return headers;
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}