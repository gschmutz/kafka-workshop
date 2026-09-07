package com.trivadis.kafkaws.orders;

import com.example.orders.avro.OrderCreated;
import com.trivadis.kafkaws.entity.Order;
import com.trivadis.kafkaws.outbox.EventPublisher;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.apache.avro.specific.SpecificRecord;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    @Value("${app.kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    @Value("${app.kafka.topics.orders}")
    private String kafkaTopic;

    @Value("${cloudevents.source}")
    private String ceSource;

    private final EventPublisher eventPublisher;

    public void send(Order order) {
        OrderCreated avroRecord = OrderCreated.newBuilder()
            .setOrderId(order.getId())
            .setCustomerId(order.getCustomerId())
            .setAmount(order.getAmount())
            .setCurrency(order.getCurrency())
            .setCreatedAt(Instant.now().toString())
            .build();

        byte[] payload = toAvroBytesSR(avroRecord);
        String subjectName = kafkaTopic + "-value";

        CloudEvent event = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(ceSource))
            .withType("com.example.order.created")
            .withTime(OffsetDateTime.now(ZoneOffset.UTC))
            .withDataContentType("avro/binary")
            .withDataSchema(URI.create(schemaRegistryUrl + "/subjects/" + subjectName + "/versions/latest"))
            .withExtension("partitionkey", order.getId())
            .withData(payload)
            .withExtension("aggregatetype", "Order")
            .build();

        eventPublisher.fire(event);
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
