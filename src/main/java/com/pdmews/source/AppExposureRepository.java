package com.pdmews.source;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AppExposureRepository extends JpaRepository<AppExposure, UUID> {
    List<AppExposure> findByUserId(UUID userId);
}
