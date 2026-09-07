# CloudEvents with Avro and Kafka from Spring Boot

This workshop demonstrates how to produce and consume **CloudEvents** in **binary content mode** over **Apache Kafka**, using **Avro** payloads registered with the **Confluent Schema Registry**.

Six producer implementations are covered, progressing from a manual approach to a fully decoupled outbox pattern.

---

## Overview

### Producers

| Project | Language | CE headers | Avro serialization | Kafka publish |
|---|---|---|---|---|
| `spring-boot-cloudevents-kafka-producer-kafka-native` | Java / Spring Boot | Manual (`RecordHeaders`) | `KafkaAvroSerializer` | Direct via `KafkaTemplate` |
| `spring-boot-cloudevents-kafka-producer-ce-native` | Java / Spring Boot | `CloudEventSerializer` (SDK) | `KafkaAvroSerializer` (manual pre-serialize) | Direct via `KafkaTemplate` |
| `spring-boot-cloudevents-kafka-outbox-kafka-native` | Java / Spring Boot | Columns in DB → Debezium | `KafkaAvroSerializer` (before DB write) | Via Debezium CDC |
| `spring-boot-cloudevents-kafka-outbox-ce-native` | Java / Spring Boot | Columns in DB → Debezium | `KafkaAvroSerializer` (before DB write) | Via Debezium CDC |
| `python-cloudevents-kafka-producer-kafka-native` | Python | Manual (`confluent-kafka` headers) | `AvroSerializer` (confluent-kafka) | Direct via `confluent_kafka.Producer` |
| `python-cloudevents-kafka-producer-ce-native` | Python | `cloudevents` SDK (`to_binary`) | `AvroSerializer` (confluent-kafka) | Direct via `confluent_kafka.Producer` |


### Consumers 

| Project | Language | CE headers | Avro serialization | Kafka publish |
|---|---|---|---|---|
| `spring-boot-cloudevents-kafka-consumer-kafka-native` | Java / Spring Boot | Manual header extraction | `KafkaAvroDeserializer` | — |

All projects share the same Avro schema and Kafka topic (`outbox.Order`).

---

## Avro Schema

All projects use the following schema (`order-created.avsc`):

```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.example.orders.avro",
  "doc": "Fired when a new order is successfully placed",
  "fields": [
    { "name": "orderId",    "type": "string" },
    { "name": "customerId", "type": "string" },
    { "name": "amount",     "type": "double" },
    { "name": "currency",   "type": "string" },
    { "name": "createdAt",  "type": "string", "doc": "ISO-8601 timestamp" }
  ]
}
```

The Maven `avro-maven-plugin` generates `com.example.orders.avro.OrderCreated` during `mvn generate-sources`.

---

## Kafka topic

All projects publish to and consume from the `outbox.Order` topic. Create it once before starting any producer or consumer:

```bash
docker exec -it kafka-1 kafka-topics \
  --bootstrap-server kafka-1:19092 \
  --create \
  --topic outbox.Order \
  --partitions 8 \
  --replication-factor 3
```

Verify it was created:

```bash
docker exec -it kafka-1 kafka-topics \
  --bootstrap-server kafka-1:19092 \
  --describe \
  --topic outbox.Order
```

---

## CloudEvents Binary Content Mode

All implementations use the [CloudEvents Kafka Protocol Binding](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md) in **binary content mode**: CE attributes are Kafka message headers and the Avro payload is the Kafka message value, using the Confluent Schema Registry compatible serialization (magic byte + schema ID prefix).

Required headers:

| Header | Example value |
|---|---|
| `ce_specversion` | `1.0` |
| `ce_id` | `550e8400-e29b-41d4-a716-446655440000` |
| `ce_type` | `com.example.orders.OrderCreated` |
| `ce_source` | `/services/order-service` |
| `ce_time` | `2026-09-07T08:00:00Z` |
| `content-type` | `avro/binary` |

Optional headers used in this workshop:

| Header | Description |
|---|---|
| `ce_dataschema` | Schema Registry URL for the Avro schema |
| `ce_subject` | Subject name in Schema Registry |
| `ce_partitionkey` | Kafka partition routing key |

---

## Consumer — Kafka Native

**Project:** `src/spring-boot-cloudevents-kafka-consumer-kafka-native`

There is a single consumer implementation shared across all producer variants. It consumes from the `outbox.Order` topic, deserializes the Avro value with `KafkaAvroDeserializer` (Schema Registry), and reads all CE metadata from the Kafka message headers.

> **Start this first and leave it running** while you test any of the producer examples below — it logs every received CloudEvent to stdout so you can verify end-to-end delivery.

### Architecture

```
Kafka topic: outbox.Order
  → KafkaAvroDeserializer → OrderCreated (SpecificRecord)
  → OrderEventListener.onOrderCreated()
       reads ce_* headers via CloudEventHeaders constants
       processes OrderCreated payload
```

### Key code — reading CE headers

`onOrderCreated` receives a `ConsumerRecord` whose value is already deserialized to `OrderCreated` by `KafkaAvroDeserializer`. All CE metadata is read from the Kafka message headers using the `CloudEventHeaders` constants and a small `header()` helper that decodes the raw bytes as UTF-8.

```java
private String header(ConsumerRecord<?, ?> record, String key) {
    return Optional.ofNullable(record.headers().lastHeader(key))
        .map(Header::value)
        .map(v -> new String(v, StandardCharsets.UTF_8))
        .orElse(null);
}
```

```java
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
```

### Run

```bash
cd src/spring-boot-cloudevents-kafka-consumer-kafka-native
mvn spring-boot:run
```

---

## Producer — Kafka Native

**Project:** `src/spring-boot-cloudevents-kafka-producer-kafka-native`

The simplest approach. CloudEvents headers are built manually using a `CloudEventHeaders` constants class and added to the Kafka `ProducerRecord`. The Avro payload is serialized by `KafkaAvroSerializer` (Confluent standard wire format with Schema Registry magic bytes).

### Architecture

```
POST /api/orders
  → OrderController
    → OrderService
      → builds OrderCreated (Avro SpecificRecord)
      → CloudEventProducer.publish()
           KafkaAvroSerializer handles value (Schema Registry)
           buildCeHeaders() builds ce_* headers manually
           ProducerRecord<String, Object> sent via KafkaTemplate
```

### Key classes

| Class | Responsibility |
|---|---|
| `CloudEventHeaders` | Constants for all `ce_*` header names and values |
| `CloudEventProducer` | Builds `RecordHeaders` and sends `ProducerRecord` |
| `KafkaProducerConfig` | Wires `KafkaAvroSerializer` as value serializer |

### Key code — building CE headers

`buildCeHeaders` constructs the CE mandatory attributes (`specversion`, `id`, `type`, `source`, `time`, `content-type`) as raw UTF-8 Kafka headers using the `CloudEventHeaders` constants. The optional `ce_dataschema` header is added only when a Schema Registry subject name is provided.

```java
private RecordHeaders buildCeHeaders(String eventType, String subjectName) {
    RecordHeaders headers = new RecordHeaders();
    headers.add(CloudEventHeaders.SPEC_VERSION, utf8("1.0"));
    headers.add(CloudEventHeaders.ID,           utf8(UUID.randomUUID().toString()));
    headers.add(CloudEventHeaders.TYPE,         utf8(eventType));
    headers.add(CloudEventHeaders.SOURCE,       utf8(defaultSource));
    headers.add(CloudEventHeaders.TIME,         utf8(Instant.now().toString()));
    headers.add(CloudEventHeaders.CONTENT_TYPE, utf8("avro/binary"));
    if (subjectName != null) {
        headers.add(CloudEventHeaders.DATA_SCHEMA,
            utf8(schemaRegistryBaseUrl + "/subjects/" + subjectName + "/versions/latest"));
    }
    return headers;
}
```

### Key code — publish() method

`publish` calls `buildCeHeaders` to assemble the CE headers and then sends a `ProducerRecord` via `KafkaTemplate`. The Avro `payload` is passed directly as the record value — `KafkaAvroSerializer` (configured in `KafkaProducerConfig`) handles the Confluent wire-format serialization transparently. The returned `CompletableFuture` logs success or failure asynchronously.

```java
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
```

### Run

```bash
cd src/spring-boot-cloudevents-kafka-producer-kafka-native
mvn spring-boot:run
```

```bash
curl -s -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-4711","amount":142.50,"currency":"CHF"}'
```

---

## Producer — CE Native (CloudEvents SDK)

**Project:** `src/spring-boot-cloudevents-kafka-producer-ce-native`

Uses the [CloudEvents Java SDK](https://github.com/cloudevents/sdk-java) (`cloudevents-kafka`) instead of manually building headers. A `CloudEvent` object is built with `CloudEventBuilder.v1()` and passed to a `KafkaTemplate<String, CloudEvent>`. The `CloudEventSerializer` (from `cloudevents-kafka`) writes the CE attributes as Kafka headers automatically.

Because `CloudEventSerializer` owns the Kafka value slot, the Avro record must be pre-serialized to Confluent wire-format bytes with `KafkaAvroSerializer` before being set as the `CloudEvent` data payload.

### Architecture

```
POST /api/orders
  → OrderController
    → OrderService
      → builds OrderCreated (Avro SpecificRecord)
      → CloudEventProducer.publish()
           toAvroBytesSR() — KafkaAvroSerializer → Confluent wire bytes
           CloudEventBuilder.v1() → CloudEvent object
           ProducerRecord<String, CloudEvent> sent via KafkaTemplate
           CloudEventSerializer writes ce_* headers + bytes as value
```

### Comparison with Kafka Native

| Aspect | Kafka Native | CE Native |
|---|---|---|
| CE header construction | Manual (`RecordHeaders`) | `CloudEventSerializer` |
| `KafkaTemplate` type | `<String, Object>` | `<String, CloudEvent>` |
| `CloudEventHeaders` class | Required | Not needed |
| SDK validation | None | `CloudEventBuilder` validates required attributes |
| Schema Registry | `KafkaAvroSerializer` as value serializer | Pre-serialized via `KafkaAvroSerializer`, then wrapped in CE |

### Key code — building and sending the CloudEvent

`publish` pre-serializes the Avro record to Confluent wire-format bytes via `toAvroBytesSR`, then builds a `CloudEvent` with `CloudEventBuilder.v1()`. The SDK validates all required CE attributes at build time and `CloudEventSerializer` writes them as Kafka headers automatically. Note that `.withExtension("partitionkey", key)` writes the `ce_partitionkey` header only — the Kafka message key must be set explicitly on the `ProducerRecord`.

```java
public <T extends SpecificRecord> CompletableFuture<SendResult<String, CloudEvent>> publish(
        String topic, String key, T payload, String eventType) {

    String subjectName = kafkaTopic + "-value";

    CloudEvent event = CloudEventBuilder.v1()
        .withId(UUID.randomUUID().toString())
        .withSource(URI.create(defaultSource))
        .withType(eventType)
        .withTime(OffsetDateTime.now())
        .withDataContentType("avro/binary")
        .withDataSchema(URI.create(schemaRegistryUrl + "/subjects/" + subjectName + "/versions/latest"))
        .withSubject(subjectName)
        .withExtension("partitionkey", key)
        .withData(toAvroBytesSR(payload))
        .build();

    // withExtension("partitionkey", ...) writes the ce_partitionkey *header* only;
    // it does NOT set the Kafka message key — pass key to ProducerRecord explicitly.
    ProducerRecord<String, CloudEvent> record = new ProducerRecord<>(topic, key, event);

    return kafkaTemplate.send(record)
        .whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish CE [type={}, key={}]: {}", eventType, key, ex.getMessage(), ex);
            } else {
                log.info("Published CE [type={}, key={}, partition={}, offset={}]",
                    eventType, key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
}
```

### Key code — toAvroBytesSR helper

Because `CloudEventSerializer` owns the Kafka value slot, the Avro record cannot be serialized by `KafkaAvroSerializer` as the value serializer. Instead, `toAvroBytesSR` manually invokes `KafkaAvroSerializer` to produce Confluent wire-format bytes (magic byte + schema ID + Avro binary), which are then set as the `CloudEvent` data payload.

```java
private <T extends SpecificRecord> byte[] toAvroBytesSR(T record) {
    SchemaRegistryClient schemaRegistryClient =
        new CachedSchemaRegistryClient(schemaRegistryUrl, 10);
    Map<String, Object> props = new HashMap<>();
    props.put(KafkaAvroSerializerConfig.AVRO_REMOVE_JAVA_PROPS_CONFIG, true);
    props.put("schema.registry.url", schemaRegistryUrl);
    KafkaAvroSerializer ser = new KafkaAvroSerializer(schemaRegistryClient, props);
    return ser.serialize(kafkaTopic, record);
}
```

### Run

```bash
cd src/spring-boot-cloudevents-kafka-producer-ce-native
mvn spring-boot:run
```

```bash
curl -s -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-4711","amount":142.50,"currency":"CHF"}'
```

---

## Producer — Outbox Pattern (Kafka Native)

**Project:** `src/spring-boot-cloudevents-kafka-outbox-kafka-native`

Solves the **dual-write problem**: directly publishing to Kafka in the same HTTP request risks Kafka succeeding while the database rolls back (or vice versa). The outbox pattern eliminates this by writing everything to PostgreSQL inside a single database transaction, and delegating the Kafka publish to **Debezium CDC**.

The application never talks to Kafka. Debezium reads the PostgreSQL Write-Ahead Log (WAL) and publishes outbox rows to Kafka, mapping dedicated CE columns to Kafka headers via the `EventRouter` SMT.

### Architecture

```
POST /api/orders
  → OrderService.placeOrder()  [@Transactional]
      ┌─────────────────────────────────────────┐
      │  Single DB transaction                  │
      │  INSERT customer_order                  │
      │  INSERT outbox (CE columns + payload)   │
      └─────────────────────────────────────────┘
      ← 201 Created

[Debezium — external Kafka Connect plugin]
  reads outbox INSERT from PostgreSQL WAL
  EventRouter SMT:
    ce_partitionkey    → Kafka message key
    payload            → Kafka message value  (Confluent Avro bytes)
    ce_id              → ce_id header + Debezium event ID
    ce_source          → ce_source header
    ce_time            → ce_time header
    ce_type            → ce_type header
    ce_specversion     → ce_specversion header
    ce_datacontenttype → content-type header
    ce_dataschema      → ce_dataschema header (optional)
    ce_subject         → ce_subject header (optional)
  publishes to topic: outbox.Order
```

### Internal event — OutboxEvent POJO

The `OrderEventProducer` builds a plain `OutboxEvent` POJO and fires it as a Spring application event.

```java
OutboxEvent event = OutboxEvent.builder()
    .ceId(order.getId())                              // → ce_id header + Debezium event ID
    .aggregateType("Order")                           // → routes to topic: outbox.Order
    .eventType("com.example.orders.OrderCreated")     // → ce_type header
    .eventKey(order.getId())                          // → Kafka message key (ce_partitionkey)
    .payload(payload)                                 // Confluent Avro wire bytes
    .ceSource(ceSource)                               // → ce_source header
    .ceTime(Instant.now())                            // → ce_time header
    .build();
eventPublisher.fire(event);
```

 `EventService` listens for it and writes the `OutboxDO` row.

### Key code — EventService.handleOutboxEvent

`handleOutboxEvent` maps each field from the `OutboxEvent` POJO onto an `OutboxDO` entity and saves it to the database. Debezium then picks up the INSERT from the PostgreSQL WAL and publishes the row as a CloudEvent to Kafka. `ce_dataschema` and `ce_subject` are set to `null` in this project — the kafka-native outbox does not carry Schema Registry metadata in dedicated columns.

```java
@EventListener
public void handleOutboxEvent(OutboxEvent event) {
    OutboxDO entity = OutboxDO.builder()
        .ceId(UUID.fromString(event.getCeId()))
        .aggregateType(event.getAggregateType())
        .ceType(event.getEventType())
        .cePartitionKey(event.getEventKey())
        .payload(event.getPayload())
        .ceSource(event.getCeSource())
        .ceTime(event.getCeTime().toString())
        .ceSpecVersion("1.0")
        .ceDataContentType("avro/binary")
        .ceDataSchema(null)
        .ceSubject(null)
        .build();
    outboxRepository.save(entity);
}
```

### Transactional guarantee

`OrderService.placeOrder()` is `@Transactional`. `EventService.handleOutboxEvent()` uses a plain `@EventListener` (not `@TransactionalEventListener`), so it fires **synchronously within the same transaction**. Both the `customer_order` and `outbox` rows commit atomically — no lost events, no phantom messages.

### Debezium connector

The connector uses the `EventRouter` SMT to route outbox rows to Kafka. Key settings:
- `table.field.event.id` / `key` / `type` point to the dedicated CE columns instead of Debezium defaults
- `route.topic.replacement` routes to `outbox.{aggregate_type}` (e.g. `outbox.Order`)
- `table.fields.additional.placement` maps every CE column to its corresponding Kafka header
- `value.converter: ByteArrayConverter` passes the Confluent Avro bytes through unmodified

```bash
curl -X PUT \
  "http://${DATAPLATFORM_IP}:8083/connectors/order-outbox-connector/config" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "tasks.max": "1",

  "database.hostname": "postgresql",
  "database.port": "5432",
  "database.user": "postgres",
  "database.password": "abc123!",
  "database.dbname": "postgres",
  "topic.prefix": "debezium",
  "schema.include.list": "public",
  "table.include.list": "public.outbox",
  "plugin.name": "pgoutput",
  "publication.name": "debezium",
  "slot.name": "debezium",
  "tombstones.on.delete": "false",

  "transforms": "outbox",
  "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
  "transforms.outbox.table.field.event.id": "ce_id",
  "transforms.outbox.table.field.event.key": "ce_partitionkey",
  "transforms.outbox.table.field.event.type": "ce_type",
  "transforms.outbox.table.field.event.payload": "payload",
  "transforms.outbox.route.by.field": "aggregate_type",
  "transforms.outbox.route.topic.replacement": "outbox.${routedByValue}",
  "transforms.outbox.table.fields.additional.placement": "ce_id:header:ce_id,ce_source:header:ce_source,ce_time:header:ce_time,ce_type:header:ce_type,ce_specversion:header:ce_specversion,ce_datacontenttype:header:content-type,ce_dataschema:header:ce_dataschema,ce_subject:header:ce_subject,ce_partitionkey:header:ce_partitionkey",

  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.converters.ByteArrayConverter",
  "topic.creation.default.replication.factor": 1,
  "topic.creation.default.partitions": 8
}'
```

> **Note:** If you add new columns to the `outbox` table after the connector was first created, you must drop and recreate the Debezium replication slot and publication — the slot caches the table schema at creation time.

```bash
cd src/spring-boot-cloudevents-kafka-outbox-kafka-native
bash src/main/resources/connector/create-connector.sh
```

### Run

```bash
cd src/spring-boot-cloudevents-kafka-outbox-kafka-native
mvn spring-boot:run
```

```bash
curl -s -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-4711","amount":142.50,"currency":"CHF"}'
```

---

## Producer — Outbox Pattern (CE Native)

**Project:** `src/spring-boot-cloudevents-kafka-outbox-ce-native`

Same outbox architecture as the Kafka Native variant, but the internal Spring application event is a proper `CloudEvent` object from the CloudEvents SDK instead of a custom `OutboxEvent` POJO. CE extension attributes carry the routing metadata (`partitionkey` for the Kafka message key, `aggregatetype` for topic routing).

### Comparison with Kafka Native

| Aspect | Kafka Native | CE Native |
|---|---|---|
| Internal event type | Custom `OutboxEvent` POJO | `CloudEvent` (SDK) |
| CE SDK dependency | None | `cloudevents-core` |
| Routing metadata | Custom fields on POJO | CE extension attributes |
| Validation | None | `CloudEventBuilder` validates required CE attributes |

### Internal event — CloudEvent with extensions

`OrderEventProducer` builds a `CloudEvent` carrying all CE attributes plus routing extensions, then fires it as a Spring application event:

```java
String subjectName = kafkaTopic + "-value";

CloudEvent event = CloudEventBuilder.v1()
    .withId(UUID.randomUUID().toString())
    .withType("com.example.orders.OrderCreated")
    .withSource(URI.create(ceSource))
    .withTime(OffsetDateTime.now(ZoneOffset.UTC))
    .withDataContentType("avro/binary")
    .withDataSchema(URI.create(schemaRegistryUrl + "/subjects/" + subjectName + "/versions/latest"))
    .withExtension("partitionkey",  order.getId())   // → Kafka message key
    .withExtension("aggregatetype", "Order")         // → routes to topic: outbox.Order
    .withData(payload)
    .build();
eventPublisher.fire(event);
```

`EventService` extracts all CE fields and saves the `OutboxDO` row using Lombok's builder:

```java
@EventListener
public void handleOutboxEvent(CloudEvent event) {
    OutboxDO entity = OutboxDO.builder()
        .ceId(UUID.fromString(event.getId()))
        .aggregateType((String) event.getExtension("aggregatetype"))
        .ceType(event.getType())
        .cePartitionKey((String) event.getExtension("partitionkey"))
        .payload(event.getData().toBytes())
        .ceSource(event.getSource().toString())
        .ceTime(event.getTime().toString())
        .ceSpecVersion("1.0")
        .ceDataContentType(event.getDataContentType())
        .ceDataSchema(event.getDataSchema() != null ? event.getDataSchema().toString() : null)
        .ceSubject(event.getSubject())
        .build();
    outboxRepository.save(entity);
}
```

### Outbox table columns (CE Native)

| Column | Type | Maps to Kafka |
|---|---|---|
| `ce_id` | `uuid` (PK) | `ce_id` header (Debezium event ID) |
| `aggregate_type` | `varchar` | topic routing (`outbox.Order`) |
| `ce_type` | `varchar` | `ce_type` header |
| `ce_partitionkey` | `varchar` | Kafka message key |
| `payload` | `bytea` | Kafka message value (Avro bytes) |
| `ce_source` | `varchar` | `ce_source` header |
| `ce_time` | `varchar` | `ce_time` header |
| `ce_specversion` | `varchar` | `ce_specversion` header |
| `ce_datacontenttype` | `varchar` | `content-type` header |
| `ce_dataschema` | `varchar` | `ce_dataschema` header |
| `ce_subject` | `varchar` | `ce_subject` header |


Then create the connector:

```bash
curl -X DELETE http://localhost:8083/connectors/order-outbox-connector
```

### Debezium connector

The connector configuration is identical to the kafka-native outbox — the same CE columns and `additional.placement` mapping are used. `value.converter: ByteArrayConverter` passes the Confluent Avro bytes through unmodified.

```bash
curl -X PUT \
  "http://${DATAPLATFORM_IP}:8083/connectors/order-outbox-connector/config" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "tasks.max": "1",

  "database.hostname": "postgresql",
  "database.port": "5432",
  "database.user": "postgres",
  "database.password": "abc123!",
  "database.dbname": "postgres",
  "topic.prefix": "debezium",
  "schema.include.list": "public",
  "table.include.list": "public.outbox",
  "plugin.name": "pgoutput",
  "publication.name": "debezium",
  "slot.name": "debezium",
  "tombstones.on.delete": "false",

  "transforms": "outbox",
  "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
  "transforms.outbox.table.field.event.id": "ce_id",
  "transforms.outbox.table.field.event.key": "ce_partitionkey",
  "transforms.outbox.table.field.event.type": "ce_type",
  "transforms.outbox.table.field.event.payload": "payload",
  "transforms.outbox.route.by.field": "aggregate_type",
  "transforms.outbox.route.topic.replacement": "outbox.${routedByValue}",
  "transforms.outbox.table.fields.additional.placement": "ce_id:header:ce_id,ce_source:header:ce_source,ce_time:header:ce_time,ce_type:header:ce_type,ce_specversion:header:ce_specversion,ce_datacontenttype:header:content-type,ce_dataschema:header:ce_dataschema,ce_subject:header:ce_subject,ce_partitionkey:header:ce_partitionkey",

  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.converters.ByteArrayConverter",
  "topic.creation.default.replication.factor": 1,
  "topic.creation.default.partitions": 8
}'
```

### Run

```bash
cd src/spring-boot-cloudevents-kafka-outbox-ce-native
mvn spring-boot:run
```

```bash
curl -s -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-4711","amount":142.50,"currency":"CHF"}'
```

---

## Producer — Python Kafka Native

**Project:** `src/python-cloudevents-kafka-producer-kafka-native`

A Python implementation of the Kafka-native approach. Uses the `confluent-kafka` library with `AvroSerializer` for Schema Registry integration and manually builds CloudEvents binary-mode headers as a list of tuples.

### Architecture

```
main.py
  → OrderService.place_order()
      → builds payload dict
      → CloudEventProducer.publish()
           AvroSerializer → Confluent wire bytes (Schema Registry)
           _build_ce_headers() → list of (key, value) tuples
           producer.produce(topic, key, value, headers)
```

### Key code

```python
def _build_ce_headers(self, event_type, subject_name=None):
    headers = [
        ("ce_specversion", "1.0"),
        ("ce_id",          str(uuid.uuid4())),
        ("ce_type",        event_type),
        ("ce_source",      self._source),
        ("ce_time",        datetime.now(timezone.utc).isoformat()),
        ("content-type",   "avro/binary"),
    ]
    if subject_name:
        headers.append(("ce_dataschema",
            f"{self._schema_registry_url}/subjects/{subject_name}/versions/latest"))
    return headers
```

### Key code — publish() method

`publish` serializes the payload dict to Confluent Avro wire-format bytes using `AvroSerializer`, builds the CE headers with `_build_ce_headers`, and produces the record via `confluent_kafka.Producer`. `poll(0)` triggers delivery callbacks without blocking.

```python
def publish(
    self,
    topic: str,
    key: str,
    payload: dict,
    schema_str: str,
    event_type: str,
    subject_name: str | None = None,
) -> None:
    serializer = self._get_serializer(schema_str)
    value_bytes = serializer(payload, SerializationContext(topic, MessageField.VALUE))

    headers = self._build_ce_headers(event_type, subject_name)

    self._producer.produce(
        topic=topic,
        key=key,
        value=value_bytes,
        headers=headers,
        on_delivery=_delivery_callback,
    )
    self._producer.poll(0)
```

### Setup and run

```bash
cd src/python-cloudevents-kafka-producer-kafka-native
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python3 main.py
```

---

## Producer — Python CE Native (CloudEvents SDK)

**Project:** `src/python-cloudevents-kafka-producer-ce-native`

Uses the [`cloudevents`](https://pypi.org/project/cloudevents/) Python SDK instead of manually building headers. A `CloudEvent` object is created and converted to Kafka binary format via `to_binary()` from `cloudevents.v1.kafka`, which returns a `KafkaMessage` with headers and value ready to pass to the producer.

### Comparison with Python Kafka Native

| Aspect | Kafka Native | CE Native |
|---|---|---|
| CE header construction | Manual list of tuples | `to_binary(event)` from SDK |
| CE object | None | `CloudEvent(attributes, data)` |
| SDK validation | None | SDK validates required CE attributes |
| Extra dependency | None | `cloudevents>=2.0.0` |

### Key code

```python
from cloudevents.v1.http import CloudEvent
from cloudevents.v1.kafka import to_binary

attributes = {
    "type":            event_type,
    "source":          self._source,
    "id":              str(uuid.uuid4()),
    "time":            datetime.now(timezone.utc).isoformat(),
    "datacontenttype": "avro/binary",
}
if subject_name:
    attributes["dataschema"] = (
        f"{self._schema_registry_url}/subjects/{subject_name}/versions/latest"
    )

event = CloudEvent(attributes=attributes, data=value_bytes)
# data_marshaller=lambda x: x — Avro bytes are already serialized, skip JSON encoding
message = to_binary(event, data_marshaller=lambda x: x)

self._producer.produce(
    topic=topic, key=key,
    value=message.value, headers=message.headers,
)
```

### Setup and run

```bash
cd src/python-cloudevents-kafka-producer-ce-native
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python3 main.py
```

## Pros and Cons

| Project | Pros | Cons |
|---|---|---|
| `producer-kafka-native` | No extra dependencies; full control over every header; easiest to debug | Error-prone (header name typos, missing headers); no SDK validation; verbose boilerplate |
| `producer-ce-native` | `CloudEventBuilder` validates required attributes; cleaner code; `partitionkey` and `dataschema` are first-class | Avro must be pre-serialized manually (SDK owns the value slot); extra `cloudevents-kafka` dependency |
| `outbox-kafka-native` | Atomic dual-write (order + outbox in one transaction); app never touches Kafka; simple `OutboxEvent` POJO | Requires Debezium + PostgreSQL WAL setup; POJO doesn't enforce CE spec; `ce_id` reuses business key |
| `outbox-ce-native` | Atomic dual-write; `CloudEventBuilder` validates the internal event; all CE attributes flow naturally from the SDK object | Most complex setup; Debezium + PostgreSQL WAL required; replication slot must be recreated when columns are added |
| `python-kafka-native` | Simple; only `confluent-kafka` needed; straightforward list of tuples | Manual header construction; no validation; transitive dependency list must be maintained explicitly |
| `python-ce-native` | `CloudEvent` object centralises attribute construction; `to_binary()` eliminates manual header tuples | `cloudevents` 2.x API is under `v1.*` namespace (breaking change); binary data requires custom `data_marshaller` workaround |

