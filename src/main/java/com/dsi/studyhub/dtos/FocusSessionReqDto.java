package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dsi.studyhub.entities.FocusSession}
 */
public record FocusSessionReqDto(
        @NotBlank String title,
        LocalDateTime timer,
        Long userId
) implements Serializable {
}
