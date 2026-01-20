package com.pdmews.risk;

import com.pdmews.source.AppExposure;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exposure_id", nullable = false)
    private AppExposure appExposure;

    // 0 to 100
    private double riskScore;

    // "HIGH", "MEDIUM", "LOW"
    private String riskLevel;

    // JSON blob or String to store "Why?"
    // For simplicity, using String. In PG we could use JSONB.
    @Column(columnDefinition = "TEXT")
    private String reasoning;

    // Feature 2: Explainable Risk Breakdown
    @Column(columnDefinition = "TEXT")
    private String riskFactorsJSON;

    @CreationTimestamp
    private LocalDateTime assessedAt;
}
