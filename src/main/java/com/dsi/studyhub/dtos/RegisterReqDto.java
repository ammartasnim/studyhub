package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record RegisterReqDto(
         @NotBlank(message = "Username is required") String username,
         @NotBlank(message = "Password is required")
         @Size(min = 8, message = "Password must be at least 8 characters")
         String password,
         @NotBlank(message = "Email is required")
         @Email(message = "Email format is invalid")
         String email,
         @NotBlank(message = "First name is required") String firstName,
         @NotBlank(message = "Last name is required") String lastName,
         String phone,
         String pfp
) implements Serializable {
}
