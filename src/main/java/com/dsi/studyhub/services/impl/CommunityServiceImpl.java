package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommunityRequestDTO;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.services.CommunityService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;

    public CommunityServiceImpl(CommunityRepository communityRepository) {
        this.communityRepository = communityRepository;
    }
    
    @Override
    public Community createCommunity(CommunityRequestDTO community) {
        Community newCommunity = new Community();
        newCommunity.setTitle(community.getTitle());
        newCommunity.setDescription(community.getDescription());
        int members = community.getNbrMembers() > 0 ? community.getNbrMembers() : 1;
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
    public Community updateCommunity(Long id, com.dsi.studyhub.dtos.CommunityRequestDTO communityDto) {
        Optional<Community> existingCommunity = communityRepository.findById(id);

        if (existingCommunity.isPresent()) {
            Community comm = existingCommunity.get();
            // Use mapper-like update logic here to respect omitted fields
            if (communityDto.getTitle() != null) {
                comm.setTitle(communityDto.getTitle());
            }
            if (communityDto.getDescription() != null) {
                comm.setDescription(communityDto.getDescription());
            }
            if (communityDto.getNbrMembers() > 0) {
                comm.setNbrMembers(communityDto.getNbrMembers());
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

