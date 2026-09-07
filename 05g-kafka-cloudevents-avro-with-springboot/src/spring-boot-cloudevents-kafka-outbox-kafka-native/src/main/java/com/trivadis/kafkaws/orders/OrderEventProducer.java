package com.trivadis.kafkaws.orders;

import com.example.orders.avro.OrderCreated;
import com.trivadis.kafkaws.entity.Order;
import com.trivadis.kafkaws.outbox.EventPublisher;
import com.trivadis.kafkaws.outbox.model.OutboxEvent;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
            .setCreatedAt(order.getCreatedAt() != null
                ? order.getCreatedAt().toString()
                : Instant.now().toString())
            .build();

        byte[] payload = serializeAvro(avroRecord);

        OutboxEvent event = OutboxEvent.builder()
            .ceId(order.getId())
            .aggregateType("Order")
            .eventType("com.example.orders.OrderCreated")
            .eventKey(order.getId())
            .payload(payload)
            .ceSource(ceSource)
            .ceTime(Instant.now())
            .ceDataSchema("")
            .ceSubject("")
            .build();

        eventPublisher.fire(event);
    }

    private byte[] serializeAvro(OrderCreated record) {
        SchemaRegistryClient client = new CachedSchemaRegistryClient(schemaRegistryUrl, 10);
        Map<String, Object> props = new HashMap<>();
        props.put(KafkaAvroSerializerConfig.AVRO_REMOVE_JAVA_PROPS_CONFIG, true);
        props.put("schema.registry.url", schemaRegistryUrl);
        KafkaAvroSerializer serializer = new KafkaAvroSerializer(client, props);
        return serializer.serialize(kafkaTopic, record);
    }

    
}
