package com.trivadis.kafkaws.outbox;

import com.trivadis.kafkaws.outbox.model.OutboxDO;
import com.trivadis.kafkaws.outbox.repository.OutboxRepository;
import io.cloudevents.CloudEvent;
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
}
