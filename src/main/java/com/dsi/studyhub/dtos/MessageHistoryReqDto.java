package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record MessageHistoryReqDto(
        @NotNull Long conversationId,
        @Min(0) Integer page,
        @Min(1) Integer size
) implements Serializable {
}
