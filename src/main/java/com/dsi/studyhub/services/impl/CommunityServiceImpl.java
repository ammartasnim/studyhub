package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommunityRequestDTO;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.services.CommunityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        newCommunity.setNbrMembers(1);
        newCommunity.setPosts(null);

        return communityRepository.save(newCommunity);
    }
    
    @Override
    public Optional<Community> getCommunityById(Long id) {
        return communityRepository.findById(id);
    }
    
    @Override
    public Page<Community> getAllCommunities(String title, String description, Integer minMembers, Pageable pageable) {
        return communityRepository.findAllWithFilters(title, description, minMembers, pageable);
    }
    
    @Override
    public Community updateCommunity(Long id, Community community) {
        Optional<Community> existingCommunity = communityRepository.findById(id);
        
        if (existingCommunity.isPresent()) {
            Community comm = existingCommunity.get();
            if (community.getTitle() != null) {
                comm.setTitle(community.getTitle());
            }
            if (community.getDescription() != null) {
                comm.setDescription(community.getDescription());
            }
            if (community.getNbrMembers() > 0) {
                comm.setNbrMembers(community.getNbrMembers());
            }
            if (community.getPosts() != null) {
                comm.setPosts(community.getPosts());
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

