package com.pdmews.event;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MisuseEventRepository extends JpaRepository<MisuseEvent, UUID> {
    List<MisuseEvent> findByUserId(UUID userId);

    // Feature 4: Spike Detection (Count events in last X hours)
    long countByUserIdAndEventTimestampAfter(UUID userId, LocalDateTime timestamp);
}
