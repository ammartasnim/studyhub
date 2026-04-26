package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.entities.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface CommunityService {
    Community createCommunity(CommunityReqDto community);
    Optional<Community> getCommunityById(Long id);
    Page<Community> getAllCommunities(String title, String description, Integer minMembers, Pageable pageable);
    Community updateCommunity(Long id, CommunityReqDto communityDto);
    void deleteCommunity(Long id);
}
