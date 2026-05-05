package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record MessageSendDto(
        @NotNull Long recipientId,
        @NotBlank String content
) implements Serializable {
}
