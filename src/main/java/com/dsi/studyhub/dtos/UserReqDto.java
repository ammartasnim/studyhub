package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
public record UserReqDto(
        String username,
        String firstName,
        String lastName,
        @Email(message = "Email format is invalid")
        String email,
        String phone
) implements Serializable {}
