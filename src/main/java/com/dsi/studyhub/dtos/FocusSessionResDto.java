package com.dsi.studyhub.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dsi.studyhub.entities.FocusSession}
 */
public record FocusSessionResDto(
        Long id,
        String title,
        String  timer,
        Integer remainingSeconds, // CRITICAL: So Angular knows where to start the countdown
        String status,           // CRITICAL: So Angular knows if it should be ticking or paused
        LocalDateTime lastUpdated, // Optional: Useful if you want to double-check sync on the frontend
        Long userId
) implements Serializable { }
