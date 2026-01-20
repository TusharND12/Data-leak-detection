package com.pdmews.timeline;

import com.pdmews.event.MisuseEvent;
import com.pdmews.event.MisuseEventRepository;
import com.pdmews.source.AppExposure;
import com.pdmews.source.AppExposureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final AppExposureRepository appExposureRepository;
    private final MisuseEventRepository misuseEventRepository;

    @GetMapping("/{userId}")
    public List<TimelineItem> getTimeline(@PathVariable UUID userId) {
        List<TimelineItem> timeline = new ArrayList<>();

        // 1. Add Exposures (Signups)
        List<AppExposure> exposures = appExposureRepository.findByUserId(userId);
        for (AppExposure e : exposures) {
            if (e.getSignupDate() != null) {
                timeline.add(TimelineItem.builder()
                        .timestamp(e.getSignupDate().atStartOfDay())
                        .type("SIGNUP")
                        .label(e.getAppName())
                        .description("User signed up for service")
                        .riskLevel("INFO")
                        .build());
            }
        }

        // 2. Add Misuse Events
        List<MisuseEvent> events = misuseEventRepository.findByUserId(userId);
        for (MisuseEvent e : events) {
            if (e.getEventTimestamp() != null) {
                timeline.add(TimelineItem.builder()
                        .timestamp(e.getEventTimestamp())
                        .type("MISUSE_EVENT")
                        .label(e.getType() != null ? e.getType().toString() : "UNKNOWN")
                        .description(e.getDescription())
                        .riskLevel(e.getSeverity() != null ? e.getSeverity().toString() : "UNKNOWN")
                        .build());
            }
        }

        // 3. Sort by Time (Newest First) - Null safe
        return timeline.stream()
                .sorted(Comparator
                        .comparing(TimelineItem::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .collect(Collectors.toList());
    }
}
