package com.pdmews.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import com.pdmews.identity.User;
import com.pdmews.identity.UserRepository;
import com.pdmews.identity.Identifier;
import com.pdmews.identity.IdentifierRepository;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class MisuseEventController {

        private final MisuseEventRepository misuseEventRepository;
        private final UserRepository userRepository;
        private final IdentifierRepository identifierRepository;

        @GetMapping("/{userId}")
        public ResponseEntity<List<MisuseEvent>> getUserEvents(@PathVariable UUID userId) {
                return ResponseEntity.ok(misuseEventRepository.findByUserId(userId));
        }

        @PostMapping
        public ResponseEntity<?> recordEvent(@RequestBody RecordEventRequest request) {
                try {
                        User user = userRepository.findById(request.userId())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        Identifier identifier = null;
                        if (request.identifierId() != null) {
                                identifier = identifierRepository.findById(request.identifierId()).orElse(null);
                        }

                        MisuseEvent event = MisuseEvent.builder()
                                        .user(user)
                                        .identifier(identifier)
                                        .type(request.type())
                                        .eventTimestamp(request.timestamp() != null ? request.timestamp()
                                                        : LocalDateTime.now())
                                        .description(request.description())
                                        .severity(request.severity())
                                        .metadata(request.metadata())
                                        .build();

                        return ResponseEntity.ok(misuseEventRepository.save(event));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body("Error recording event: " + e.getMessage());
                }
        }

        public record RecordEventRequest(
                        UUID userId,
                        UUID identifierId,
                        MisuseEvent.EventType type,
                        LocalDateTime timestamp,
                        String description,
                        MisuseEvent.Severity severity,
                        String metadata) {
        }
}
