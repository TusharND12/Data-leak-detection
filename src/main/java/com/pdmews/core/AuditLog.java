package com.pdmews.core;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    private String entityType;

    private UUID entityId;

    @Builder.Default
    private String actor = "SYSTEM";

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
