package com.pdmews.legal;

import com.pdmews.risk.RiskAssessment;
import com.pdmews.risk.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForensicService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final EvidenceRecordRepository evidenceRecordRepository;

    @Transactional
    public EvidenceRecord preserveEvidence(UUID riskAssessmentId) {
        RiskAssessment assessment = riskAssessmentRepository.findById(riskAssessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Risk Assessment not found"));

        // Check if evidence already exists (Idempotency)
        var existing = evidenceRecordRepository.findByRiskAssessment(assessment);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 1. Create the Snapshot String (The "Content")
        String snapshot = "UserID:" + assessment.getAppExposure().getUser().getId() +
                "|App:" + assessment.getAppExposure().getAppName() +
                "|Risk:" + assessment.getRiskScore() +
                "|Reason:" + assessment.getReasoning() +
                "|Date:" + assessment.getAssessedAt();

        // 2. Generate Cryptographic Hash (SHA-256)
        String hash = generateSha256(snapshot);

        // 3. Generate Legal Text
        String legalText = generateLegalNotice(assessment, hash);

        // 4. Save
        EvidenceRecord record = EvidenceRecord.builder()
                .riskAssessment(assessment)
                .contentHash(hash)
                .legalNoticeText(legalText)
                .build();

        return evidenceRecordRepository.save(record);
    }

    private String generateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String generateLegalNotice(RiskAssessment assessment, String hash) {
        return """
                NOTICE OF DATA MISUSE / GDPR ARTICLE 21 OBJECTION

                To the Data Protection Officer of: %s

                I am a user of your service. My internal user reference is: %s.

                I have detected a high correlation of personal data misuse originating from your platform.

                EVIDENCE SUMMARY:
                - Risk Score: %.2f / 100
                - Detection Date: %s
                - Digital Forensic Hash: %s

                This record is cryptographically frozen.

                Pursuant to GDPR Article 17 (Right to Erasure) and Article 21 (Right to Object),
                I hereby demand immediate deletion of all my personal data.

                Failure to comply may result in a formal complaint to the relevant Data Protection Authority.
                """.formatted(
                assessment.getAppExposure().getAppName(),
                assessment.getAppExposure().getUser().getId(),
                assessment.getRiskScore(),
                assessment.getAssessedAt(),
                hash);
    }
}
