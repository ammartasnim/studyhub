package com.dsi.studyhub.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public record NotificationResDto(
        Long id,
        String type,
        String message,
        String link,
        Long refId,
        boolean isRead,
        LocalDateTime createdAt
) implements Serializable {
}
