package com.pdmews.risk;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByUserId(UUID userId);
}
