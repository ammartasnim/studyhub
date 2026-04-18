package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.services.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommunityServiceImpl implements CommunityService {
    
    @Autowired
    private CommunityRepository communityRepository;
    
    @Override
    public Community createCommunity(Community community) {
        return communityRepository.save(community);
    }
    
    @Override
    public Optional<Community> getCommunityById(Long id) {
        return communityRepository.findById(id);
    }
    
    @Override
    public List<Community> getAllCommunities() {
        return communityRepository.findAll();
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
            if (community.getPost() != null) {
                comm.setPost(community.getPost());
            }
            return communityRepository.save(comm);
        }
        
        return null;
    }
    
    @Override
    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }
}

