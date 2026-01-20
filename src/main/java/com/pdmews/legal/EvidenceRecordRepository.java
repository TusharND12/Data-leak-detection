package com.pdmews.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import com.pdmews.risk.RiskAssessment;
import java.util.Optional;

public interface EvidenceRecordRepository extends JpaRepository<EvidenceRecord, UUID> {
    Optional<EvidenceRecord> findByRiskAssessment(RiskAssessment riskAssessment);
}
