package com.pdmews.risk;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Alert>> getUserAlerts(@PathVariable UUID userId) {
        return ResponseEntity.ok(alertRepository.findByUserId(userId));
    }
}
