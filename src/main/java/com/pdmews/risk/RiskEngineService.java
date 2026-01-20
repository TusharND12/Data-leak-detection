package com.pdmews.risk;

import com.pdmews.event.MisuseEvent;
import com.pdmews.event.MisuseEventRepository;
// Removed AppSource imports
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.pdmews.source.AppExposure;
import com.pdmews.source.AppExposureRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEngineService {

    private final AppExposureRepository appExposureRepository;
    private final MisuseEventRepository misuseEventRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AlertRepository alertRepository;
    private final GlobalTrustService globalTrustService;
    private final FederatedRiskService federatedRiskService;

    @Transactional
    public List<RiskAssessment> analyzeUserRisk(UUID userId) {
        log.info("Starting hypothesis-based risk analysis for user: {}", userId);

        List<AppExposure> sources = appExposureRepository.findByUserId(userId);
        List<MisuseEvent> events = misuseEventRepository.findByUserId(userId);

        if (sources.isEmpty() || events.isEmpty()) {
            // Check for direct data leak events (unattributed to any user-entered app)
            List<MisuseEvent> leaks = events.stream()
                    .filter(e -> e.getType() == MisuseEvent.EventType.DATA_LEAK)
                    .collect(Collectors.toList());
            if (!leaks.isEmpty()) {
                return List.of(createDataLeakAssessment(userId, leaks.get(0)));
            }
            log.info("Insufficient data (no apps or no misuse signals) for user: {}", userId);
            return List.of();
        }

        List<RiskAssessment> assessments = new ArrayList<>();
        for (AppExposure source : sources) {
            assessments.add(calculateRiskForSource(source, events));
        }

        // Rank by confidence score descending
        assessments.sort(Comparator.comparingDouble(RiskAssessment::getRiskScore).reversed());

        // Process and save all assessments (even low trust ones for historical
        // tracking)
        List<RiskAssessment> processed = assessments.stream()
                .map(this::processAssessment)
                .collect(Collectors.toList());

        // According to core principles, we return all as candidates,
        // but the UI/Client will use reasoning/thresholds to display them.
        return processed;
    }

    private RiskAssessment processAssessment(RiskAssessment assessment) {
        RiskAssessment saved = riskAssessmentRepository.save(assessment);
        if (saved.getRiskScore() > 70.0) {
            createAlert(saved);
            globalTrustService.decreaseTrust(saved.getAppExposure().getAppName(), 1);
            federatedRiskService.reportHighRisk(saved.getAppExposure().getAppName());
        }
        return saved;
    }

    private RiskAssessment calculateRiskForSource(AppExposure source, List<MisuseEvent> events) {
        double totalScore = 0.0;
        StringBuilder reasoning = new StringBuilder();
        StringBuilder factorsJson = new StringBuilder("{");

        reasoning.append("### Hypothesis: ").append(source.getAppName()).append(" as Source\n");
        reasoning.append("Evaluating evidence for this hypothesis...\n\n");

        // 1. Signup Time Correlation (40% Weight)
        double timeScore = calculateTimeCorrelationScore(source, events, reasoning, factorsJson);

        // 2. Frequency Spike Detection (20% Weight)
        double spikeScore = calculateFrequencySpikeScore(source, events, reasoning, factorsJson);

        // 3. Category / Domain Matching (15% Weight Bonus)
        double categoryBonus = calculateCategoryMatchBonus(source, events, reasoning, factorsJson);

        // 4. Federated Risk / Cross-User Correlation (25% Weight)
        double globalRiskScore = calculateFederatedRiskScore(source, reasoning, factorsJson);

        // 5. App Trust Score Adjustment (Informational)
        double trustAdjustment = calculateTrustRiskScore(source, reasoning, factorsJson);
        log.debug("Informational Trust Adjustment for {}: {}", source.getAppName(), trustAdjustment);

        // 6. Direct Breach Match (Override)
        double breachMatchScore = calculateDirectBreachMatch(source, events, reasoning, factorsJson);

        // Synthesize final score using core logic weights
        totalScore = (timeScore * 0.40) + (spikeScore * 0.20) + (globalRiskScore * 0.25) + categoryBonus;

        if (breachMatchScore >= 100.0) {
            totalScore = 100.0;
            // reasoning and factorsJson are updated inside calculateDirectBreachMatch
        }

        totalScore = Math.min(100.0, Math.max(0.0, totalScore));

        factorsJson.append("\"final_score\": ").append(String.format("%.2f", totalScore)).append("}");

        // Threshold-based explainability
        if (totalScore < 40.0) {
            reasoning.append("\n**Result**: Insufficient evidence. App does not align with misuse patterns.\n");
        } else if (totalScore < 65.0) {
            reasoning.append("\n**Result**: Monitoring. Possible correlation detected but requires more signals.\n");
        } else {
            reasoning.append("\n**Result**: High Probability. Strong alignment with observed misuse.\n");
        }

        reasoning.append("\n**Final Confidence: ").append(String.format("%.1f", totalScore)).append("%**");

        return RiskAssessment.builder()
                .appExposure(source)
                .riskScore(totalScore)
                .riskLevel(determineLevel(totalScore))
                .reasoning(reasoning.toString())
                .riskFactorsJSON(factorsJson.toString())
                .build();
    }

    private double calculateDirectBreachMatch(AppExposure source, List<MisuseEvent> events, StringBuilder reasoning,
            StringBuilder json) {
        String appName = source.getAppName();
        if (appName == null)
            return 0.0;

        for (MisuseEvent event : events) {
            if (event.getType() == MisuseEvent.EventType.DATA_LEAK && event.getMetadata() != null) {
                // Simple check for "breached_app": "AppName"
                // We could use Jackson for full parsing, but this handles the current format
                if (event.getMetadata().contains("\"breached_app\": \"" + appName + "\"")) {
                    reasoning.insert(0, "- **CRITICAL MATCH**: Direct data breach detected at " + appName + ".\n");
                    json.append("\"direct_breach_match\": 100.0, ");
                    return 100.0;
                }
            }
        }
        json.append("\"direct_breach_match\": 0.0, ");
        return 0.0;
    }

    private double calculateTimeCorrelationScore(AppExposure source, List<MisuseEvent> events, StringBuilder reasoning,
            StringBuilder json) {
        double maxScore = 0.0;
        LocalDateTime signup = source.getSignupDate().atStartOfDay();

        for (MisuseEvent event : events) {
            long days = Duration.between(signup, event.getEventTimestamp()).toDays();
            double current = 0.0;
            if (days >= 0 && days <= 2)
                current = 100.0;
            else if (days > 2 && days <= 7)
                current = 80.0;
            else if (days > 7 && days <= 14)
                current = 50.0;
            else if (days > 14 && days <= 30)
                current = 20.0;

            if (current > maxScore)
                maxScore = current;
        }

        reasoning.append("- **Time Correlation**: ")
                .append(maxScore > 0 ? "High suspicion due to recent signup proximity." : "Low temporal link.")
                .append("\n");
        json.append("\"time_correlation_score\": ").append(maxScore).append(", ");
        return maxScore;
    }

    private double calculateFrequencySpikeScore(AppExposure source, List<MisuseEvent> events, StringBuilder reasoning,
            StringBuilder json) {
        LocalDateTime signup = source.getSignupDate().atStartOfDay();
        LocalDateTime windowEnd = signup.plusDays(90); // 90-day critical attribution window

        long preCount = events.stream().filter(e -> e.getEventTimestamp().isBefore(signup)).count();
        long postCount = events.stream().filter(e -> e.getEventTimestamp().isAfter(signup)).count();

        // New: Count events specifically within the 90-day window after signup
        long windowCount = events.stream()
                .filter(e -> e.getEventTimestamp().isAfter(signup) && e.getEventTimestamp().isBefore(windowEnd))
                .count();

        double spikeScore = 0.0;
        if (windowCount > 0) {
            spikeScore = 100.0;
            reasoning.append(
                    "- **Frequency Spike**: Detected misuse events within 90 days of signup (High Attribution).\n");
        } else if (postCount > preCount * 2 && postCount > 0) {
            spikeScore = 5.0; // Reduced to minimal impact for generic spikes
            reasoning.append(
                    "- **Frequency Spike**: Generic increase in misuse detected (Low Attribution; likely unrelated to this app).\n");
        } else if (postCount > preCount) {
            spikeScore = 2.0;
            reasoning.append("- **Frequency Spike**: Minor increase in misuse detected.\n");
        } else {
            reasoning.append("- **Frequency Spike**: No significant change in misuse frequency.\n");
        }

        json.append("\"frequency_spike_score\": ").append(spikeScore).append(", ");
        return spikeScore;
    }

    private double calculateCategoryMatchBonus(AppExposure source, List<MisuseEvent> events, StringBuilder reasoning,
            StringBuilder json) {
        double bonus = 0.0;
        String category = source.getCategory();

        for (MisuseEvent event : events) {
            if (category == null)
                break;

            if (category.equals("FINANCE") && (event.getType() == MisuseEvent.EventType.SPAM_SMS
                    || event.getType() == MisuseEvent.EventType.PHISHING_ATTEMPT)) {
                bonus = 15.0;
            } else if (category.equals("SOCIAL") && event.getType() == MisuseEvent.EventType.SPAM_EMAIL) {
                bonus = 10.0;
            }
        }

        if (bonus > 0) {
            reasoning.append("- **Category Match**: App category (").append(category)
                    .append(") aligns with observed misuse type (+").append(bonus).append("% boost).\n");
        }
        json.append("\"category_match_bonus\": ").append(bonus).append(", ");
        return bonus;
    }

    private double calculateFederatedRiskScore(AppExposure source, StringBuilder reasoning, StringBuilder json) {
        double multiplier = federatedRiskService.getCrowdMultiplier(source.getAppName());
        double score = (multiplier - 1.0) * 200.0; // scale 1.5x multiplier to 100 points
        score = Math.min(100.0, score);

        if (score > 10) {
            reasoning.append("- **Cross-User Consensus**: Multiple users reported similar patterns with this app.\n");
        }
        json.append("\"federated_risk_score\": ").append(score).append(", ");
        return score;
    }

    private double calculateTrustRiskScore(AppExposure source, StringBuilder reasoning, StringBuilder json) {
        int trust = globalTrustService.getTrustScore(source.getAppName());
        double trustRisk = 100.0 - trust;

        reasoning.append("- **App Reputation**: Trust score is ").append(trust).append("/100.\n");
        json.append("\"trust_score\": ").append(trust).append(", ");
        return trustRisk;
    }

    private String determineLevel(double score) {
        if (score >= 80)
            return "CRITICAL";
        if (score >= 60)
            return "HIGH";
        if (score >= 40)
            return "MEDIUM";
        return "LOW";
    }

    private void createAlert(RiskAssessment assessment) {
        Alert alert = Alert.builder()
                .user(assessment.getAppExposure().getUser())
                .title("Early Warning: " + assessment.getAppExposure().getAppName())
                .message("High probability (" + String.format("%.1f", assessment.getRiskScore())
                        + "%) of data source mismatch detected.")
                .severity(assessment.getRiskLevel())
                .build();
        alertRepository.save(alert);
    }

    private RiskAssessment createDataLeakAssessment(UUID userId, MisuseEvent leak) {
        // Create a virtual app source for the leak representation
        AppExposure virtualSource = AppExposure.builder()
                .appName("Identity Monitor (Breach Detected)")
                .category("IDENTITY")
                .signupDate(java.time.LocalDate.now())
                .status(AppExposure.SourceStatus.ACTIVE)
                .build();

        return RiskAssessment.builder()
                .appExposure(virtualSource)
                .riskScore(100.0)
                .riskLevel("CRITICAL")
                .reasoning("CRITICAL: Identity found in data breach.\nDetails: " + leak.getDescription())
                .riskFactorsJSON("{\"event_type\": \"DATA_LEAK\", \"impact\": 100}")
                .build();
    }
}
