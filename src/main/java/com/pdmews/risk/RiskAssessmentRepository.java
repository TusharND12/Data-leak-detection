package com.pdmews.risk;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {
}
