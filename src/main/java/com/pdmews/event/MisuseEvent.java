package com.pdmews.event;

import com.pdmews.identity.Identifier;
import com.pdmews.identity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "misuse_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MisuseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identifier_id", nullable = true)
    private Identifier identifier;

    @Column(nullable = false)
    private LocalDateTime eventTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    // e.g. "received spam call", "phishing link"
    private String description;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    // Optional: stores metadata for correlation (e.g., sender domain, call
    // category)
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum EventType {
        SPAM_CALL, SPAM_SMS, SPAM_EMAIL, PHISHING_ATTEMPT, DATA_LEAK, UNKNOWN
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
