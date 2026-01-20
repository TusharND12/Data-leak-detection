package com.pdmews.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
public class LegalController {

    private final ForensicService forensicService;

    @PostMapping("/preserve/{riskAssessmentId}")
    public ResponseEntity<EvidenceRecord> preserveEvidence(@PathVariable UUID riskAssessmentId) {
        return ResponseEntity.ok(forensicService.preserveEvidence(riskAssessmentId));
    }

    // Added for effortless Browser Demo
    @GetMapping("/preserve-demo/{riskAssessmentId}")
    public ResponseEntity<EvidenceRecord> preserveEvidenceDemo(@PathVariable UUID riskAssessmentId) {
        return ResponseEntity.ok(forensicService.preserveEvidence(riskAssessmentId));
    }
}
