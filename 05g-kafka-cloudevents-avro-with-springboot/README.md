# CloudEvents with Avro and Kafka from Spring Boot

This workshop demonstrates how to produce and consume **CloudEvents** in **binary content mode** over **Apache Kafka**, using **Avro** payloads registered with the **Confluent Schema Registry**.

Seven implementations are covered, progressing from a manual approach to a fully decoupled outbox pattern.

---

## Overview

| Project | Language | CE headers | Avro serialization | Kafka publish |
|---|---|---|---|---|
| `spring-boot-cloudevents-kafka-producer-kafka-native` | Java / Spring Boot | Manual (`RecordHeaders`) | `KafkaAvroSerializer` | Direct via `KafkaTemplate` |
| `spring-boot-cloudevents-kafka-consumer-kafka-native` | Java / Spring Boot | Manual header extraction | `KafkaAvroDeserializer` | — |
| `spring-boot-cloudevents-kafka-producer-ce-native` | Java / Spring Boot | `CloudEventSerializer` (SDK) | `KafkaAvroSerializer` (manual pre-serialize) | Direct via `KafkaTemplate` |
| `spring-boot-cloudevents-kafka-outbox-kafka-native` | Java / Spring Boot | Columns in DB → Debezium | `KafkaAvroSerializer` (before DB write) | Via Debezium CDC |
| `spring-boot-cloudevents-kafka-outbox-ce-native` | Java / Spring Boot | Columns in DB → Debezium | `KafkaAvroSerializer` (before DB write) | Via Debezium CDC |
| `python-cloudevents-kafka-producer-kafka-native` | Python | Manual (`confluent-kafka` headers) | `AvroSerializer` (confluent-kafka) | Direct via `confluent_kafka.Producer` |
| `python-cloudevents-kafka-producer-ce-native` | Python | `cloudevents` SDK (`to_binary`) | `AvroSerializer` (confluent-kafka) | Direct via `confluent_kafka.Producer` |

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

## CloudEvents Binary Content Mode

All implementations use the [CloudEvents Kafka Protocol Binding](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md) in **binary content mode**: CE attributes are Kafka message headers and the Avro payload is the Kafka message value.

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

## Producer — Kafka Native

**Project:** `src/spring-boot-cloudevents-kafka-producer-kafka-native`

The simplest approach. CloudEvents headers are built manually using a `CloudEventHeaders` constants class and added to the Kafka `ProducerRecord`. The Avro payload is serialized by `KafkaAvroSerializer` (Confluent wire format with Schema Registry magic bytes).

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

## Consumer — Kafka Native

**Project:** `src/spring-boot-cloudevents-kafka-consumer-kafka-native`

Consumes from the `outbox.Order` topic. Deserializes the Avro value with `KafkaAvroDeserializer` (Schema Registry) and reads CE metadata from the Kafka message headers.

### Architecture

```
Kafka topic: outbox.Order
  → KafkaAvroDeserializer → OrderCreated (SpecificRecord)
  → OrderEventListener.onOrderCreated()
       reads ce_* headers via CloudEventHeaders constants
       processes OrderCreated payload
```

### Key code — reading CE headers

```java
@KafkaListener(topics = "${app.kafka.topics.orders}", ...)
public void onOrderCreated(ConsumerRecord<String, OrderCreated> record) {
    String ceSpecVersion  = header(record, CloudEventHeaders.SPEC_VERSION);
    String ceId           = header(record, CloudEventHeaders.ID);
    String ceType         = header(record, CloudEventHeaders.TYPE);
    String ceSource       = header(record, CloudEventHeaders.SOURCE);
    String ceTime         = header(record, CloudEventHeaders.TIME);
    String ceDataSchema   = header(record, CloudEventHeaders.DATA_SCHEMA);
    String ceSubject      = header(record, CloudEventHeaders.SUBJECT);
    String cePartitionKey = header(record, CloudEventHeaders.PARTITION_KEY);
    String contentType    = header(record, CloudEventHeaders.CONTENT_TYPE);
    OrderCreated order    = record.value();
    // process order ...
}

private String header(ConsumerRecord<?, ?> record, String key) {
    return Optional.ofNullable(record.headers().lastHeader(key))
        .map(Header::value)
        .map(v -> new String(v, StandardCharsets.UTF_8))
        .orElse(null);
}
```

### Run

```bash
cd src/spring-boot-cloudevents-kafka-consumer-kafka-native
mvn spring-boot:run
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

```java
String subjectName = kafkaTopic + "-value";

CloudEvent event = CloudEventBuilder.v1()
    .withId(UUID.randomUUID().toString())
    .withType(eventType)
    .withSource(URI.create(defaultSource))
    .withTime(OffsetDateTime.now())
    .withDataContentType("avro/binary")
    .withDataSchema(URI.create(schemaRegistryUrl + "/subjects/" + subjectName + "/versions/latest"))
    .withSubject(subjectName)
    .withExtension("partitionkey", key)
    .withData(toAvroBytesSR(payload))
    .build();

// Derive Kafka message key from the CE extension rather than the local variable
String messageKey = (String) event.getExtension("partitionkey");
ProducerRecord<String, CloudEvent> record =
    new ProducerRecord<>(topic, messageKey, event);
kafkaTemplate.send(record);
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

The `OrderEventProducer` builds a plain `OutboxEvent` POJO and fires it as a Spring application event. `EventService` listens for it and writes the `OutboxDO` row.

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

### Transactional guarantee

`OrderService.placeOrder()` is `@Transactional`. `EventService.handleOutboxEvent()` uses a plain `@EventListener` (not `@TransactionalEventListener`), so it fires **synchronously within the same transaction**. Both the `customer_order` and `outbox` rows commit atomically — no lost events, no phantom messages.

### Debezium connector

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

### Debezium setup notes

After adding new columns to the outbox table, the replication slot must be recreated to pick up the new schema:

```sql
ALTER TABLE public.outbox REPLICA IDENTITY FULL;
DROP PUBLICATION IF EXISTS debezium;
CREATE PUBLICATION debezium FOR TABLE public.outbox;
SELECT pg_drop_replication_slot('debezium');  -- if slot still exists
```

Then recreate the connector:

```bash
curl -X DELETE http://localhost:8083/connectors/order-outbox-connector
bash src/main/resources/connector/create-connector.sh
```

### Debezium connector

```bash
cd src/spring-boot-cloudevents-kafka-outbox-ce-native
bash src/main/resources/connector/create-connector.sh
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

### Dependencies (`requirements.txt`)

```
cloudevents>=2.0.0
confluent-kafka>=2.3.0
fastavro>=1.9.0
certifi>=2024.0.0
httpx>=0.27.0
authlib>=1.3.0
cachetools>=5.3.0
attrs>=23.0.0
jsonschema>=4.22.0
referencing>=0.35.0
protobuf>=5.27.0
```

> Note: `confluent-kafka` 2.15+ has undeclared transitive dependencies. All required packages are explicitly listed above.
> Note: `cloudevents` 2.x moved all bindings under `cloudevents.v1.*` — use `cloudevents.v1.http` and `cloudevents.v1.kafka`, not `cloudevents.http`/`cloudevents.kafka`.

---

## Prerequisites

- Java 21
- Maven 3.8+
- Python 3.11+ (for the Python projects)
- Running Kafka cluster at `localhost:9092`
- Confluent Schema Registry at `http://localhost:8081`
- PostgreSQL at `localhost:5432/postgres` (for the outbox projects)
- Debezium Kafka Connect plugin (for the outbox projects)
