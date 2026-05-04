package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.CommunityPermission;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public record CommunityResDto(
        Long id,
        String title,
        String description,
        int nbrMembers,
        Long ownerId,
        String category,
        List<ModeratorInfo> moderators
) implements Serializable {

    public record ModeratorInfo(
            Long userId,
            String username,
            String fullName,
            Set<CommunityPermission> permissions
    ) implements Serializable {}
}