package com.pdmews.timeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineItem {
    private LocalDateTime timestamp;
    private String type; // "SIGNUP" or "MISUSE_EVENT"
    private String label; // App Name or Event Type
    private String description;
    private String riskLevel; // For events
}
