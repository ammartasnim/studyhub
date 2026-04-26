package com.dsi.studyhub.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record ErrorResDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) implements Serializable {
}
