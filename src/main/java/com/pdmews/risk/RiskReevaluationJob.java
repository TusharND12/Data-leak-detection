package com.pdmews.risk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.pdmews.identity.UserRepository;
import com.pdmews.identity.User;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskReevaluationJob {

    private final RiskEngineService riskEngineService;
    private final UserRepository userRepository;

    /**
     * Periodically re-evaluates risk for all active users.
     * Runs every 6 hours in a real system. Demo: every 5 minutes if configured.
     */
    @Scheduled(fixedRateString = "${pdmews.risk.reevaluation-rate:3600000}")
    public void reevaluateAllUsers() {
        log.info("Starting background risk re-evaluation job...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                riskEngineService.analyzeUserRisk(user.getId());
            } catch (Exception e) {
                log.error("Failed to re-evaluate risk for user: {}", user.getId(), e);
            }
        }
        log.info("Background risk re-evaluation complete.");
    }
}
