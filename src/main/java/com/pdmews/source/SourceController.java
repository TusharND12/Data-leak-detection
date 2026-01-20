package com.pdmews.source;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exposures")
@RequiredArgsConstructor
public class SourceController {

    private final AppExposureRepository appExposureRepository;
    private final com.pdmews.identity.UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<List<AppExposure>> getUserExposures(@PathVariable UUID userId) {
        return ResponseEntity.ok(appExposureRepository.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<AppExposure> addExposure(@RequestBody CreateAppExposureRequest request) {
        com.pdmews.identity.User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        AppExposure exposure = AppExposure.builder()
                .user(user)
                .appName(request.appName())
                .signupDate(request.signupDate())
                .category(request.category())
                .status(AppExposure.SourceStatus.ACTIVE)
                .build();

        return ResponseEntity.ok(appExposureRepository.save(exposure));
    }

    public record CreateAppExposureRequest(
            UUID userId,
            String appName,
            java.time.LocalDate signupDate,
            String category) {
    }
}
