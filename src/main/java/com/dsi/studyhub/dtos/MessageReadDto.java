package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record MessageReadDto(
        @NotNull Long conversationId
) implements Serializable {
}
