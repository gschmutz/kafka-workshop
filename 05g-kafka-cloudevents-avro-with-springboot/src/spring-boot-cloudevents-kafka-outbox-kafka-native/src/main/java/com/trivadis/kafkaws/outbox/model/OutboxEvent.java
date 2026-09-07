package com.trivadis.kafkaws.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class OutboxEvent {
    private final String ceId;
    private final String aggregateType;
    private final String eventType;       // becomes ce_type header
    private final String eventKey;        // becomes Kafka message key and partitionkey
    private final byte[] payload;         // Confluent wire-format Avro bytes
    private final String ceSource;        // becomes ce_source header
    private final Instant ceTime;         // becomes ce_time header
    private final String ceDataSchema;    // becomes ce_dataschema header
    private final String ceSubject;       // becomes ce_subject header
}
