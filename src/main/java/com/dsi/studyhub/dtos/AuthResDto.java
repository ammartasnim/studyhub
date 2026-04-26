package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.UserRole;

import java.io.Serializable;

public record AuthResDto(
        String token,
        Long userId,
        String username,
        UserRole role
) implements Serializable {
}
