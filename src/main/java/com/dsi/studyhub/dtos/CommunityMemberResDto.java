package com.dsi.studyhub.dtos;

import java.io.Serializable;

public record CommunityMemberResDto(
        Long userId,
        String username,
        String fullName,
        String pfp,
        int xpPts,
        int level,
        boolean isModerator,
        long warningCount
) implements Serializable {}