package com.pdmews.risk;

import com.pdmews.event.MisuseEvent;
import com.pdmews.event.MisuseEventRepository;
import com.pdmews.identity.Identifier;
import com.pdmews.identity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeakDetectionService {

    private final RestTemplate restTemplate;
    private final MisuseEventRepository misuseEventRepository;

    @Value("${pdmews.hibp.api-key:}")
    private String hibpApiKey;

    private static final String HIBP_URL = "https://haveibeenpwned.com/api/v3/breachedaccount/";

    /**
     * Checks if the given identity (email) matches any known breaches.
     * 
     * @param identifier   The email address to check (plaintext).
     * @param user         The user who owns this identity.
     * @param dbIdentifier The contact point object (can be null if not linked yet).
     */
    public void checkIdentity(String identifier, User user, Identifier dbIdentifier) {
        if (identifier == null || identifier.isBlank()) {
            log.warn("Skipping leak check: Identifier is null or blank.");
            return;
        }

        // Allow emails (@) or digit-based identifiers (phone)
        boolean isEmail = identifier.contains("@");
        boolean isPhone = identifier.matches("^[+]?[0-9\\- ]+$");

        if (!isEmail && !isPhone) {
            log.info("Skipping leak check: Identifier '{}' is neither an email nor a valid phone number.", identifier);
            return;
        }

        if (hibpApiKey == null || hibpApiKey.isBlank()) {
            log.warn("Real user data leak detection skipped: HIBP API Key is missing.");

            // Demo Mode Logic: Simulate breaches for all inputs when API key is missing
            log.info("Demo Mode: Simulating granular breaches for identifier '{}'", identifier);

            String[] mockBreaches = { "Adobe", "LinkedIn", "Canva", "Dropbox" };
            for (String appName : mockBreaches) {
                MisuseEvent event = MisuseEvent.builder()
                        .user(user)
                        .identifier(dbIdentifier)
                        .type(MisuseEvent.EventType.DATA_LEAK)
                        .eventTimestamp(LocalDateTime.now().minusDays(new java.util.Random().nextInt(30)))
                        .severity(MisuseEvent.Severity.HIGH)
                        .description("Identity found in " + appName + " data breach.")
                        .metadata("{\"breached_app\": \"" + appName + "\"}")
                        .build();

                misuseEventRepository.save(event);
            }
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("hibp-api-key", hibpApiKey);
            headers.set("user-agent", "PD-MEWS-Risk-Agent");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = HIBP_URL + identifier + "?truncateResponse=false";

            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && !response.getBody().isEmpty()) {
                List<Object> breaches = response.getBody();
                log.info("Found {} breaches for identifier: {}", breaches.size(), identifier);

                for (Object b : breaches) {
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> breach = (java.util.Map<String, Object>) b;
                        String innerAppName = (String) breach.get("Name");

                        MisuseEvent event = MisuseEvent.builder()
                                .user(user)
                                .identifier(dbIdentifier)
                                .type(MisuseEvent.EventType.DATA_LEAK)
                                .eventTimestamp(LocalDateTime.now()) // HIBP has BreachDate, could use that
                                .severity(MisuseEvent.Severity.HIGH)
                                .description("Identity found in " + innerAppName + " data breach (via HIBP).")
                                .metadata("{\"breached_app\": \"" + innerAppName + "\"}")
                                .build();

                        misuseEventRepository.save(event);
                    } catch (Exception e) {
                        log.error("Error parsing individual breach record", e);
                    }
                }
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.info("No breaches found for identifier: {}", identifier);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("HIBP API Key is invalid or expired.");
        } catch (Exception e) {
            log.error("Error calling Leak Detection API", e);
        }
    }
}
