package com.pdmews.risk;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature 6: Cross-User Correlation (Federated Defense).
 * Allows the system to learn from the "Wisdom of the Crowd".
 * If multiple users report the same app, the risk score increases for everyone.
 */
@Service
public class FederatedRiskService {

    // K: AppName, V: Count of High Risk detections across all users
    private static final Map<String, Integer> GLOBAL_RISK_CACHE = new ConcurrentHashMap<>();

    // Called when a High Risk alert is generated for ANY user
    public void reportHighRisk(String appName) {
        GLOBAL_RISK_CACHE.merge(appName, 1, Integer::sum);
    }

    // Called during analysis to see if OTHERS have flagged this app
    public double getCrowdMultiplier(String appName) {
        int reports = GLOBAL_RISK_CACHE.getOrDefault(appName, 0);

        // Logic: 0 reports = 1.0 (Neutral)
        // 1 report = 1.0 (Isolated incident)
        // 5 reports = 1.2 (20% Boost)
        // 10+ reports = 1.5 (50% Boost - Major warning)
        if (reports <= 1)
            return 1.0;
        if (reports <= 5)
            return 1.0 + (reports * 0.04); // e.g. 5 * 0.04 = 0.2 -> 1.2x
        return 1.5; // Cap at 1.5x
    }

    public int getReportCount(String appName) {
        return GLOBAL_RISK_CACHE.getOrDefault(appName, 0);
    }
}
