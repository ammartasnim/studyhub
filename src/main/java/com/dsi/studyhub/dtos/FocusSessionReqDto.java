package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
public record FocusSessionReqDto(
        @NotBlank String title,
        String timer,
        Integer remainingSeconds,
        Long userId
) implements Serializable { }
