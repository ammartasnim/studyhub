package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record ChangePasswordDto(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword,
        @NotBlank(message = "Please confirm your new password")
        String confirmPassword
) implements Serializable {}