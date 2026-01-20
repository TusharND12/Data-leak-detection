package com.pdmews.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface IdentifierRepository extends JpaRepository<Identifier, UUID> {
    List<Identifier> findByUserId(UUID userId);
}
