package com.pdmews.risk;

import com.pdmews.event.MisuseEvent;
import com.pdmews.event.MisuseEventRepository;
import com.pdmews.source.AppExposure;
import com.pdmews.source.AppExposureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTest {

    @Mock
    private AppExposureRepository appExposureRepository;
    @Mock
    private MisuseEventRepository misuseEventRepository; // Restored
    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private FederatedRiskService federatedRiskService; // Added Mock
    @Spy
    private GlobalTrustService globalTrustService;

    @InjectMocks
    private RiskEngineService riskEngineService;

    @BeforeEach
    void setUp() {
        when(riskAssessmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void testHighRiskCorrelation() {
        UUID userId = UUID.randomUUID();

        AppExposure badApp = AppExposure.builder()
                .id(UUID.randomUUID())
                .user(null)
                .appName("BadApp")
                .signupDate(LocalDate.of(2024, 1, 1))
                .build();

        MisuseEvent spamEvent = MisuseEvent.builder()
                .id(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.of(2024, 1, 2, 10, 0))
                .type(MisuseEvent.EventType.SPAM_CALL)
                .build();

        when(appExposureRepository.findByUserId(userId)).thenReturn(List.of(badApp));
        when(misuseEventRepository.findByUserId(userId)).thenReturn(List.of(spamEvent));

        // Mock Federated multiplier
        when(federatedRiskService.getCrowdMultiplier(any())).thenReturn(1.0);

        List<RiskAssessment> results = riskEngineService.analyzeUserRisk(userId);
        assertTrue(!results.isEmpty());
        RiskAssessment result = results.get(0);

        // Total: 40 + 20 + 0 + 5 = 65.0

        System.out.println("Reasoning: " + result.getReasoning());
        System.out.println("Score: " + result.getRiskScore());

        assertEquals(65.0, result.getRiskScore(), 0.1);
        assertEquals("HIGH", result.getRiskLevel());
    }

    @Test
    void testLowRiskDueToTimeGap() {
        UUID userId = UUID.randomUUID();

        AppExposure oldApp = AppExposure.builder()
                .appName("OldApp")
                .signupDate(LocalDate.of(2023, 1, 1))
                .build();

        MisuseEvent spamEvent = MisuseEvent.builder()
                .eventTimestamp(LocalDateTime.of(2024, 1, 2, 10, 0))
                .type(MisuseEvent.EventType.SPAM_CALL)
                .build();

        when(appExposureRepository.findByUserId(userId)).thenReturn(List.of(oldApp));
        when(misuseEventRepository.findByUserId(userId)).thenReturn(List.of(spamEvent));
        when(federatedRiskService.getCrowdMultiplier(any())).thenReturn(1.0);

        List<RiskAssessment> results = riskEngineService.analyzeUserRisk(userId);
        assertTrue(!results.isEmpty());
        RiskAssessment result = results.get(0);

        // Calculation V3:
        // TimeScore: 0 (diff > 30) -> 0 * 0.4 = 0.0
        // TrustScore: "OldApp" (50) -> Risk 50 * 0.1 = 5.0
        // Frequency: Spike detected -> 100 * 0.2 = 20.0
        // Total: 0 + 5 + 20 = 25.0

        assertEquals(25.0, result.getRiskScore(), 0.1);
        assertEquals("LOW", result.getRiskLevel());
    }
}
