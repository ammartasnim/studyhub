package com.dsi.studyhub.services;

import com.dsi.studyhub.enums.CommunityPermission;

public interface CommunityAuthService {
    void requireOwner(Long userId, Long communityId);
    void requireOwnerOrPermission(Long userId, Long communityId, CommunityPermission permission);
    boolean isOwner(Long userId, Long communityId);
    boolean isOwnerOrHasPermission(Long userId, Long communityId, CommunityPermission permission);
    boolean isModerator(Long userId, Long communityId);
    void requireOwnerOrModerator(Long userId, Long communityId);
    boolean hasPermission(Long userId, Long communityId, CommunityPermission permission);
}
