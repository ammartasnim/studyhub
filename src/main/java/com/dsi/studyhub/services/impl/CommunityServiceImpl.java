package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.mappers.CommunityMapper;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommunityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMapper communityMapper;
    private final AuthenticatedUserService authenticatedUserService;

    public CommunityServiceImpl(CommunityRepository communityRepository,
                                CommunityMapper communityMapper,
                                AuthenticatedUserService authenticatedUserService) {
        this.communityRepository = communityRepository;
        this.communityMapper = communityMapper;
        this.authenticatedUserService = authenticatedUserService;
    }
    
    @Override
    public Community createCommunity(CommunityReqDto community) {
        User moderator = authenticatedUserService.getAuthenticatedUser();
        boolean hasExplorerBadge = moderator.getBadges().stream()
                .anyMatch(badge -> badge.getType() == BadgeType.EXPLORER);

        if (!hasExplorerBadge) {
            throw new ForbiddenException("Explorer badge is required to create a community");
        }

        Community newCommunity = communityMapper.toEntity(community);
        int members = community.nbrMembers() != null && community.nbrMembers() > 0 ? community.nbrMembers() : 1;
        newCommunity.setNbrMembers(members);
        newCommunity.setPosts(null);
        newCommunity.setModerator(moderator);
        return communityRepository.save(newCommunity);
    }
    
    @Override
    public Optional<Community> getCommunityById(Long id) {
        return communityRepository.findById(id);
    }
    
    @Override
    public Page<Community> getAllCommunities(String title, String description, Integer minMembers, Pageable pageable) {
        return communityRepository.findByFilters(title, description, minMembers, pageable);
    }
    
    @Override
    public Community updateCommunity(Long id, CommunityReqDto communityDto) {
        Optional<Community> existingCommunity = communityRepository.findById(id);

        if (existingCommunity.isPresent()) {
            Community comm = existingCommunity.get();
            communityMapper.partialUpdate(communityDto, comm);
            if (communityDto.nbrMembers() != null && communityDto.nbrMembers() <= 0) {
                comm.setNbrMembers(1);
            }
            return communityRepository.save(comm);
        }

        throw new ResourceNotFoundException("Community not found with id: " + id);
    }
    
    @Override
    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }
}

