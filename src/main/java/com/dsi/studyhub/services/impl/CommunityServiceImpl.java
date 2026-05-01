package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.mappers.CommunityMapper;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommunityService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
    public Page<CommunityResDto> getMyCommunities(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();

        return communityRepository.findAllJoinedOrModerated(currentUser.getId(), pageable)
                .map(communityMapper::toDto);
    }

    @Override
    public Page<CommunityResDto> getMyCreatedCommunities(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();

        return communityRepository.findModerated(currentUser.getId(), pageable)
                .map(communityMapper::toDto);
    }

    @Override
    public Page<CommunityResDto> getAllCommunities(String title, String description, Integer minMembers, Pageable pageable) {
        return communityRepository.findByFilters(title, description, minMembers, pageable)
                .map(communityMapper::toDto);
    }
    
    @Override
    public Community updateCommunity(Long id, CommunityReqDto communityDto) {
        Optional<Community> existingCommunity = communityRepository.findById(id);

        if (existingCommunity.isPresent()) {
            Community comm = existingCommunity.get();
            User currentUser = authenticatedUserService.getAuthenticatedUser();
            
            if (!comm.getModerator().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Only the moderator can update this community.");
            }

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
        Optional<Community> existingCommunity = communityRepository.findById(id);
        if (existingCommunity.isPresent()) {
            Community comm = existingCommunity.get();
            User currentUser = authenticatedUserService.getAuthenticatedUser();
            
            if (!comm.getModerator().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Only the moderator can delete this community.");
            }
            
            communityRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Community not found with id: " + id);
        }
    }

    @Override
    public Map<String, Long> getCommunityStats() {
        return Map.of("total", communityRepository.count());
    }

    @Override
    public List<CommunityResDto> getTopCommunities() {
        return communityRepository.findTopByMemberCount(PageRequest.of(0, 5))
                .stream()
                .map(communityMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void joinCommunity(Long communityId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found with id: " + communityId));

        if (community.getMembers().contains(currentUser)) {
            throw new IllegalStateException("You are already a member of this community.");
        }

        community.getMembers().add(currentUser);
        currentUser.getJoinedCommunities().add(community);
        community.setNbrMembers(community.getNbrMembers() + 1);
        communityRepository.save(community);
    }

    @Override
    @Transactional
    public void leaveCommunity(Long communityId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found with id: " + communityId));

        if (!community.getMembers().contains(currentUser)) {
            throw new IllegalStateException("You are not a member of this community.");
        }

        if (community.getModerator().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are the moderator — transfer or delete the community instead of leaving.");
        }

        community.getMembers().remove(currentUser);
        currentUser.getJoinedCommunities().remove(community);
        community.setNbrMembers(Math.max(1, community.getNbrMembers() - 1));
        communityRepository.save(community);
    }
}

