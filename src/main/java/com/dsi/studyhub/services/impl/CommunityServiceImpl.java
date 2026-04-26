package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.mappers.CommunityMapper;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.services.CommunityService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMapper communityMapper;

    public CommunityServiceImpl(CommunityRepository communityRepository, CommunityMapper communityMapper) {
        this.communityRepository = communityRepository;
        this.communityMapper = communityMapper;
    }
    
    @Override
    public Community createCommunity(CommunityReqDto community) {
        Community newCommunity = communityMapper.toEntity(community);
        int members = community.nbrMembers() != null && community.nbrMembers() > 0 ? community.nbrMembers() : 1;
        newCommunity.setNbrMembers(members);
        newCommunity.setPosts(null);
        return communityRepository.save(newCommunity);
    }
    
    @Override
    public Optional<Community> getCommunityById(Long id) {
        return communityRepository.findById(id);
    }
    
    @Override
    public List<Community> getAllCommunities(String title, String description, Integer minMembers, Pageable pageable) {
        // Delegate to repository query that handles optional filters and paging
        return communityRepository.findByFilters(title, description, minMembers, pageable).getContent();
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

        throw new IllegalArgumentException("Community not found with id: " + id);
    }
    
    @Override
    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }
}

