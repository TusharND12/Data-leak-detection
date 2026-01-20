package com.pdmews.risk;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.pdmews.identity.User;
import com.pdmews.identity.UserRepository;
import com.pdmews.identity.Identifier;
import com.pdmews.identity.IdentifierRepository;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskEngineService riskEngineService;
    private final LeakDetectionService leakDetectionService;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final UserRepository userRepository;
    private final IdentifierRepository identifierRepository;

    // In a real JWT setup, userId comes from the SecurityContext
    @GetMapping("/analyze/{userId}")
    public ResponseEntity<List<RiskAssessment>> analyzeRisk(@PathVariable UUID userId) {
        return ResponseEntity.ok(riskEngineService.analyzeUserRisk(userId));
    }

    @GetMapping("/explain/{assessmentId}")
    public ResponseEntity<RiskAssessment> getRiskExplanation(@PathVariable UUID assessmentId) {
        return ResponseEntity.ok(riskAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found")));
    }

    @PostMapping("/check-leak")
    public ResponseEntity<String> checkLeak(@RequestBody LeakCheckRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Match Contact Point by Hash (Simple SHA256)
        // In a real production app, we would use a specific Bean or Util for this.
        String hash = hashIdentifier(request.email());

        List<Identifier> identifiers = identifierRepository.findByUserId(request.userId());

        Identifier matchedIdentifier = identifiers.stream()
                .filter(cp -> cp.getIdentifierHash().equals(hash))
                .findFirst()
                .orElse(null);

        leakDetectionService.checkIdentity(request.email(), user, matchedIdentifier);

        return ResponseEntity.ok("Leak check initiated. If breaches are found, they will appear in your alerts.");
    }

    private String hashIdentifier(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public record LeakCheckRequest(UUID userId, String email) {
    }
}
