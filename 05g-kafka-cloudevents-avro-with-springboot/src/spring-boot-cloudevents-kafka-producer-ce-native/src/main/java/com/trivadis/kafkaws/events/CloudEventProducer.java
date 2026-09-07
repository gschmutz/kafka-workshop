package com.trivadis.kafkaws.events;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudEventProducer {

    @Value("${app.kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    @Value("${app.kafka.topics.orders}")
    private String kafkaTopic;

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;

    @Value("${cloudevents.source}")
    private String defaultSource;

    public <T extends SpecificRecord> CompletableFuture<SendResult<String, CloudEvent>> publish(
            String topic,
            String key,
            T payload,
            String eventType) {

        String subjectName = kafkaTopic + "-value";

        CloudEvent event = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(defaultSource))
            .withType(eventType)
            .withTime(OffsetDateTime.now())
            .withDataContentType("avro/binary")
            .withDataSchema(URI.create(schemaRegistryUrl + "/subjects/" + subjectName + "/versions/latest"))
            .withSubject("subject")
            .withExtension("partitionkey", key)
            .withData(toAvroBytesSR(payload))
            .build();

        String messageKey = (String) event.getExtension("partitionkey");
        ProducerRecord<String, CloudEvent> record =
            new ProducerRecord<>(topic, key, event);

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

    /**
     * Version without schema registry support 
     */
    private <T extends SpecificRecord> byte[] toAvroBytes(T record) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DatumWriter<T> writer = new SpecificDatumWriter<>(record.getSchema());
            Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T extends SpecificRecord> byte[] toAvroBytesSR(T record) {
        SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 10);
        Map<String, Object> props = new HashMap<>();
        // send correct schemas to the registry, without "avro.java.string"
        props.put(KafkaAvroSerializerConfig.AVRO_REMOVE_JAVA_PROPS_CONFIG, true);
        props.put("schema.registry.url", schemaRegistryUrl);
        KafkaAvroSerializer ser = new KafkaAvroSerializer(schemaRegistryClient, props);
        return ser.serialize(kafkaTopic, record);
    }    
}
