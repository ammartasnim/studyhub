package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.UserRole;

import java.io.Serializable;
import java.util.List;
public record UserResDto(
        Long id,
        String username,
        String pfp,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        String phone,
        int xpPts,
        int level,
        boolean banned,
        List<BadgeResDto> badges
) implements Serializable {
}
