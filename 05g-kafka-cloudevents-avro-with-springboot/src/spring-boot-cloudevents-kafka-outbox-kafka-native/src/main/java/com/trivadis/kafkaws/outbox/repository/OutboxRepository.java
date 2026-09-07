package com.trivadis.kafkaws.outbox.repository;

import com.trivadis.kafkaws.outbox.model.OutboxDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxDO, UUID> {}
