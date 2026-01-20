package com.pdmews.identity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "identifiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Identifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactType type;

    // We store the hash of the email/phone for privacy.
    // In a real system, we might encourage salt or dedicated hashing service.
    @Column(name = "identifier_hash", nullable = false)
    private String identifierHash;

    // Optional: a friendly label (e.g. "My Personal Email")
    private String label;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ContactType {
        EMAIL, PHONE
    }
}
