package com.dsi.studyhub.services;

import com.dsi.studyhub.enums.CommunityPermission;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.CommunityModeratorRepository;
import com.dsi.studyhub.repositories.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityAuthService {

    private final CommunityRepository communityRepository;
    private final CommunityModeratorRepository communityModeratorRepository;

    // Throws if user is not the owner
    public void requireOwner(Long userId, Long communityId) {
        boolean isOwner = communityRepository.findById(communityId)
                .map(c -> c.getOwner() != null && c.getOwner().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        if (!isOwner) {
            throw new ForbiddenException("Only the community owner can perform this action.");
        }
    }

    // Throws if user is not owner AND does not have the required permission
    public void requireOwnerOrPermission(Long userId, Long communityId, CommunityPermission permission) {
        boolean isOwner = communityRepository.findById(communityId)
                .map(c -> c.getOwner() != null && c.getOwner().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        if (isOwner) return;

        boolean hasPerm = communityModeratorRepository.hasPermission(userId, communityId, permission);
        if (!hasPerm) {
            throw new ForbiddenException("You don't have permission to perform this action.");
        }
    }

    // Returns true if owner, false if not (no exception — use for conditional logic)
    public boolean isOwner(Long userId, Long communityId) {
        return communityRepository.findById(communityId)
                .map(c -> c.getOwner() != null && c.getOwner().getId().equals(userId))
                .orElse(false);
    }

    // Returns true if owner OR has the permission
    public boolean isOwnerOrHasPermission(Long userId, Long communityId, CommunityPermission permission) {
        if (isOwner(userId, communityId)) return true;
        return communityModeratorRepository.hasPermission(userId, communityId, permission);
    }

    // Returns true if user is a moderator (regardless of permissions)
    public boolean isModerator(Long userId, Long communityId) {
        return communityModeratorRepository.existsByUserIdAndCommunityId(userId, communityId);
    }

    // Throws if user has no involvement in community (not owner, not moderator)
    public void requireOwnerOrModerator(Long userId, Long communityId) {
        if (!isOwner(userId, communityId) && !isModerator(userId, communityId)) {
            throw new ForbiddenException("You must be an owner or moderator to perform this action.");
        }
    }
}