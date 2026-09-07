package com.trivadis.kafkaws.outbox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Outbox table entity. Each row represents one CloudEvent to be forwarded to Kafka by Debezium.
 *
 * Debezium EventRouter SMT reads these columns:
 *   id            → ce_id Kafka header (also the internal Debezium event ID)
 *   aggregate_id  → used for routing / tracing
 *   aggregate_type→ determines target Kafka topic  (outbox.<aggregate_type>)
 *   event_type    → ce_type Kafka header
 *   event_key     → Kafka message key
 *   payload       → Kafka message value (Confluent Avro wire-format bytes)
 *   ce_source     → ce_source Kafka header
 *   ce_time       → ce_time Kafka header  (ISO-8601 string)
 *   ce_specversion→ ce_specversion Kafka header (always "1.0")
 *   ce_datacontenttype → content-type Kafka header (always "avro/binary")
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxDO {

    @Id
    @Column(name = "ce_id", nullable = false)
    private UUID ceId;

//    @Column(name = "aggregate_id", nullable = false)
//    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "ce_type", nullable = false)
    private String ceType;

    @Column(name = "ce_partitionkey")
    private String cePartitionKey;

    @Column(name = "payload", nullable = false)
    private byte[] payload;

    @Column(name = "ce_source", nullable = false)
    private String ceSource;

    @Column(name = "ce_time", nullable = false)
    private String ceTime;

    @Column(name = "ce_specversion", nullable = false)
    private String ceSpecVersion;

    @Column(name = "ce_datacontenttype", nullable = false)
    private String ceDataContentType;

    @Column(name = "ce_dataschema", nullable = true)
    private String ceDataSchema;   

    @Column(name = "ce_subject", nullable = true)
    private String ceSubject; 
}
