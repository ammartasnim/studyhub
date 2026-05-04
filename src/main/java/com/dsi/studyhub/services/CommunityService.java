package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.CommunityMemberResDto;
import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.entities.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CommunityService {
    Community createCommunity(CommunityReqDto community);
    Optional<Community> getCommunityById(Long id);
    Page<CommunityResDto> getMyCommunities(Pageable pageable);
    Page<CommunityResDto> getMyCreatedCommunities(Pageable pageable);
    Page<CommunityResDto> getAllCommunities(String title, String description, Integer minMembers, Pageable pageable);
    Community updateCommunity(Long id, CommunityReqDto communityDto);
    void deleteCommunity(Long id);
    Map<String, Long> getCommunityStats();
    List<CommunityResDto> getTopCommunities();
    void joinCommunity(Long communityId);
    void leaveCommunity(Long communityId);
    void addModerator(Long communityId, CommunityReqDto.AddModeratorReq request);
    void removeModerator(Long communityId, Long userId);
    void updateModeratorPermissions(Long communityId, Long userId, CommunityReqDto.UpdatePermissionsReq request);
    void transferOwnership(Long communityId, CommunityReqDto.TransferOwnershipReq request);
    Page<CommunityMemberResDto> getMembers(Long communityId, Pageable pageable);
    void banMember(Long communityId, CommunityReqDto.BanMemberReq request);
    void unbanMember(Long communityId, Long userId);
    void warnMember(Long communityId, CommunityReqDto.WarnMemberReq request);
}