package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record LoginReqDto(
            @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) implements Serializable {
}
