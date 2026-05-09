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

    // Ownership checks
    public void requireOwner(Long userId, Long communityId) {
        boolean isOwner = communityRepository.findById(communityId)
                .map(c -> c.getOwner() != null && c.getOwner().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        if (!isOwner) {
            throw new ForbiddenException("Only the community owner can perform this action.");
        }
    }

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

    // Permission checks
    public boolean isOwner(Long userId, Long communityId) {
        return communityRepository.findById(communityId)
                .map(c -> c.getOwner() != null && c.getOwner().getId().equals(userId))
                .orElse(false);
    }

    public boolean isOwnerOrHasPermission(Long userId, Long communityId, CommunityPermission permission) {
        if (isOwner(userId, communityId)) return true;
        return communityModeratorRepository.hasPermission(userId, communityId, permission);
    }

    public boolean isModerator(Long userId, Long communityId) {
        return communityModeratorRepository.existsByUserIdAndCommunityId(userId, communityId);
    }

    public void requireOwnerOrModerator(Long userId, Long communityId) {
        if (!isOwner(userId, communityId) && !isModerator(userId, communityId)) {
            throw new ForbiddenException("You must be an owner or moderator to perform this action.");
        }
    }
    public boolean hasPermission(Long userId, Long communityId, CommunityPermission permission) {
        try {
            requireOwnerOrPermission(userId, communityId, permission);
            return true;
        } catch (ForbiddenException e) {
            return false;
        }
    }
}
