package com.pdmews.legal;

import com.pdmews.risk.RiskAssessment;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "evidence_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class EvidenceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // One-to-One: One risk assessment = One evidence record
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_assessment_id", nullable = false)
    private RiskAssessment riskAssessment;

    // The SHA-256 Hash of the snapshot data at the time of freezing
    @Column(nullable = false, length = 64)
    private String contentHash;

    // The legal text generated
    @Column(columnDefinition = "TEXT")
    private String legalNoticeText;

    @CreationTimestamp
    private LocalDateTime preservedAt;
}
