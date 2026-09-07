package com.trivadis.kafkaws.outbox;

import com.trivadis.kafkaws.outbox.model.OutboxDO;
import com.trivadis.kafkaws.outbox.model.OutboxEvent;
import com.trivadis.kafkaws.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final OutboxRepository outboxRepository;

    /**
     * Plain @EventListener fires synchronously within the caller's transaction,
     * so the Order insert and outbox insert share the same DB transaction — atomicity guaranteed.
     */
    @EventListener
    public void handleOutboxEvent(OutboxEvent event) {

        OutboxDO entity = OutboxDO.builder()
            .ceId(UUID.fromString(event.getCeId()))
            .aggregateType((String) event.getAggregateType())
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
        // Row is NOT deleted — Debezium reads from the PostgreSQL WAL, not by polling.
        // The row acts as a durable event log. Add a cleanup job if table growth is a concern.
    }
}
