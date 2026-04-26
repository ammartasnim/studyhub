package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * DTO for {@link com.dsi.studyhub.entities.User}
 */
public record UserReqDto(
        @NotBlank String username,
        @NotBlank String password,
        String pfp,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        UserRole role,
        String phone,
        Integer xpPts,
        Integer level,
        Boolean banned
) implements Serializable {
}
