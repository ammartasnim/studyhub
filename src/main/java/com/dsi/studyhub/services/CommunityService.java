package com.dsi.studyhub.services;

import com.dsi.studyhub.entities.Community;
import java.util.List;
import java.util.Optional;

public interface CommunityService {
    
    Community createCommunity(Community community);
    
    Optional<Community> getCommunityById(Long id);
    
    List<Community> getAllCommunities();
    
    Community updateCommunity(Long id, Community community);
    
    void deleteCommunity(Long id);
}

