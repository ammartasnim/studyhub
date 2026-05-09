package com.dsi.studyhub.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
public record FocusSessionResDto(
        Long id,
        String title,
        String  timer,
        Integer remainingSeconds,
        String status,
        LocalDateTime lastUpdated,
        Long userId
) implements Serializable { }
