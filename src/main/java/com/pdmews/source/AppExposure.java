package com.pdmews.source;

import com.pdmews.identity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "app_exposures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class AppExposure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String appName;

    @Column(nullable = false)
    private LocalDate signupDate;

    // e.g., FINANCE, SOCIAL, ECOMMERCE
    private String category;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SourceStatus status = SourceStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum SourceStatus {
        ACTIVE, INACTIVE, DELETED
    }
}
