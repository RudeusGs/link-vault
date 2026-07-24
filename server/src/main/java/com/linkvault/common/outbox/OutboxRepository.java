package com.linkvault.common.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' OR (o.status = 'FAILED' AND o.createdAt > :cutoff)")
    List<OutboxEvent> findPendingOrRecentFailed(java.time.Instant cutoff, Pageable pageable);
}
