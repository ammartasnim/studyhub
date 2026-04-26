package com.dsi.studyhub.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dsi.studyhub.entities.FocusSession}
 */
public record FocusSessionResDto(
        Long id,
        String title,
        LocalDateTime timer,
        Long userId
) implements Serializable {
}
