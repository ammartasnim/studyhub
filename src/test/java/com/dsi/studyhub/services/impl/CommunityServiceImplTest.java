package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommunityMemberResDto;
import com.dsi.studyhub.dtos.CommunityModerationDto;
import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.entities.Badge;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.CommunityBan;
import com.dsi.studyhub.entities.CommunityModerator;
import com.dsi.studyhub.entities.CommunityWarning;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.enums.CommunityPermission;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.mappers.CommunityMapper;
import com.dsi.studyhub.repositories.CommunityBanRepository;
import com.dsi.studyhub.repositories.CommunityModeratorRepository;
import com.dsi.studyhub.repositories.CommunityRepository;
import com.dsi.studyhub.repositories.CommunityWarningRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommunityAuthService;
import com.dsi.studyhub.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityMapper communityMapper;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private CommunityBanRepository communityBanRepository;
    @Mock private CommunityWarningRepository communityWarningRepository;
    @Mock private CommunityAuthService communityAuthService;
    @Mock private UserRepository userRepository;
    @Mock private CommunityModeratorRepository communityModeratorRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private CommunityServiceImpl communityService;

    private User owner;
    private User member;
    private Community community;

    // Builds shared fixtures to keep each test focused on a single rule.
    @BeforeEach
    void setUp() {
        owner = buildUser(1L, "owner", "Owner", "One");
        member = buildUser(2L, "member", "Member", "Two");
        community = buildCommunity(10L, owner, 3);
        community.getMembers().add(member);
    }

    // Enforces the explorer badge requirement for creating new communities.
    @Test
    void createCommunity_requiresExplorerBadge() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);

        assertThatThrownBy(() -> communityService.createCommunity(new CommunityReqDto("Title", "Desc", 1, "cat")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Explorer badge");
    }

    // Allows explorers to create communities and defaults member count to 1.
    @Test
    void createCommunity_withExplorerBadge_createsAndDefaultsMembers() {
        owner.getBadges().add(new Badge(owner, 1L, BadgeType.EXPLORER));
        CommunityReqDto req = new CommunityReqDto("Title", "Desc", 0, "cat");
        Community mapped = buildCommunity(null, owner, 0);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityMapper.toEntity(req)).thenReturn(mapped);
        when(communityRepository.save(mapped)).thenReturn(mapped);

        Community result = communityService.createCommunity(req);

        assertThat(result.getNbrMembers()).isEqualTo(1);
        assertThat(result.getOwner()).isEqualTo(owner);
    }

    // Exposes the raw repository Optional for direct reads.
    @Test
    void getCommunityById_returnsOptional() {
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));

        Optional<Community> result = communityService.getCommunityById(10L);

        assertThat(result).contains(community);
    }

    // Confirms member listings are scoped to the authenticated user.
    @Test
    void getMyCommunities_usesAuthenticatedUser() {
        Pageable pageable = PageRequest.of(0, 5);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findAllJoinedOrModerated(owner.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(community)));
        when(communityMapper.toDto(community)).thenReturn(new CommunityResDto(10L, "Title", "Desc", 3, owner.getId(), "cat", List.of()));

        Page<CommunityResDto> result = communityService.getMyCommunities(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Confirms creator listings are scoped to the authenticated user.
    @Test
    void getMyCreatedCommunities_usesOwnerId() {
        Pageable pageable = PageRequest.of(0, 5);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findByOwner(owner.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(community)));
        when(communityMapper.toDto(community)).thenReturn(new CommunityResDto(10L, "Title", "Desc", 3, owner.getId(), "cat", List.of()));

        Page<CommunityResDto> result = communityService.getMyCreatedCommunities(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Verifies filters are passed to the repository and mapped.
    @Test
    void getAllCommunities_passesFilters() {
        Pageable pageable = PageRequest.of(0, 5);
        when(communityRepository.findByFilters("Title", "Desc", 2, pageable))
                .thenReturn(new PageImpl<>(List.of(community)));
        when(communityMapper.toDto(community)).thenReturn(new CommunityResDto(10L, "Title", "Desc", 3, owner.getId(), "cat", List.of()));

        Page<CommunityResDto> result = communityService.getAllCommunities("Title", "Desc", 2, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Confirms community updates require owner or delegated permission.
    @Test
    void updateCommunity_requiresPermission() {
        CommunityReqDto req = new CommunityReqDto("New", "Desc", -1, "cat");
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.save(community)).thenReturn(community);

        Community result = communityService.updateCommunity(10L, req);

        verify(communityAuthService).requireOwnerOrPermission(owner.getId(), 10L, CommunityPermission.EDIT_COMMUNITY);
        verify(communityMapper).partialUpdate(req, community);
        assertThat(result.getNbrMembers()).isEqualTo(1);
    }

    // Ensures deletion enforces strict ownership before removing records.
    @Test
    void deleteCommunity_requiresOwner() {
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);

        communityService.deleteCommunity(10L);

        verify(communityAuthService).requireOwner(owner.getId(), 10L);
        verify(communityRepository).deleteById(10L);
    }

    // Prevents banned users from joining a community.
    @Test
    void joinCommunity_bannedUser_throwsForbidden() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(member);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(communityBanRepository.existsByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(true);

        assertThatThrownBy(() -> communityService.joinCommunity(10L))
                .isInstanceOf(ForbiddenException.class);
    }

    // Avoids duplicate memberships and keeps member counts consistent.
    @Test
    void joinCommunity_alreadyMember_throwsIllegalState() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(member);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(communityBanRepository.existsByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.joinCommunity(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already a member");
    }

    // Adds members and increments counts for valid joins.
    @Test
    void joinCommunity_success_addsMember() {
        User newUser = buildUser(3L, "new", "New", "User");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(newUser);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(communityBanRepository.existsByUserIdAndCommunityId(newUser.getId(), 10L)).thenReturn(false);

        communityService.joinCommunity(10L);

        assertThat(community.getMembers()).contains(newUser);
        assertThat(community.getNbrMembers()).isEqualTo(4);
        verify(communityRepository).save(community);
    }

    // Ensures only members can leave communities.
    @Test
    void leaveCommunity_notMember_throwsIllegalState() {
        User outsider = buildUser(9L, "outsider", "Out", "Side");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(outsider);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));

        assertThatThrownBy(() -> communityService.leaveCommunity(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a member");
    }

    // Prevents owners from leaving without transferring ownership.
    @Test
    void leaveCommunity_ownerCannotLeave() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));

        assertThatThrownBy(() -> communityService.leaveCommunity(10L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("owner");
    }

    // Removes members and decrements counts with a minimum of 1.
    @Test
    void leaveCommunity_success_removesMember() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(member);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));

        communityService.leaveCommunity(10L);

        assertThat(community.getMembers()).doesNotContain(member);
        assertThat(community.getNbrMembers()).isEqualTo(2);
        verify(communityRepository).save(community);
    }

    // Adds a moderator when permission is granted and target is valid.
    @Test
    void addModerator_success_sendsNotification() {
        User newMod = buildUser(4L, "mod", "Mod", "Erator");
        CommunityReqDto.AddModeratorReq req = new CommunityReqDto.AddModeratorReq(newMod.getId(), Set.of());
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(newMod.getId())).thenReturn(Optional.of(newMod));
        when(communityModeratorRepository.existsByUserIdAndCommunityId(newMod.getId(), 10L)).thenReturn(false);

        communityService.addModerator(10L, req);

        verify(communityAuthService).requireOwnerOrPermission(owner.getId(), 10L, CommunityPermission.ADD_MODERATOR);
        verify(communityModeratorRepository).save(any(CommunityModerator.class));
        verify(notificationService).createNotification(eq(newMod.getId()), eq("MODERATOR"), any(), isNull(), eq(10L));
    }

    // Prevents adding the owner as a moderator.
    @Test
    void addModerator_ownerAsModerator_throwsIllegalState() {
        CommunityReqDto.AddModeratorReq req = new CommunityReqDto.AddModeratorReq(owner.getId(), Set.of());
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> communityService.addModerator(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Owner");
    }

    // Blocks duplicate moderator additions.
    @Test
    void addModerator_alreadyModerator_throwsIllegalState() {
        User newMod = buildUser(4L, "mod", "Mod", "Erator");
        CommunityReqDto.AddModeratorReq req = new CommunityReqDto.AddModeratorReq(newMod.getId(), Set.of());
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(newMod.getId())).thenReturn(Optional.of(newMod));
        when(communityModeratorRepository.existsByUserIdAndCommunityId(newMod.getId(), 10L)).thenReturn(true);

        assertThatThrownBy(() -> communityService.addModerator(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already a moderator");
    }

    // Removes moderators only when they exist.
    @Test
    void removeModerator_missingModerator_throwsNotFound() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityModeratorRepository.existsByUserIdAndCommunityId(9L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.removeModerator(10L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Confirms moderator removal invokes the repository delete by ids.
    @Test
    void removeModerator_success_deletes() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityModeratorRepository.existsByUserIdAndCommunityId(2L, 10L)).thenReturn(true);

        communityService.removeModerator(10L, 2L);

        verify(communityAuthService).requireOwnerOrPermission(owner.getId(), 10L, CommunityPermission.REMOVE_MODERATOR);
        verify(communityModeratorRepository).deleteByCommunityIdAndUserId(10L, 2L);
    }

    // Ensures only owners can update moderator permissions.
    @Test
    void updateModeratorPermissions_requiresOwner() {
        CommunityReqDto.UpdatePermissionsReq req = new CommunityReqDto.UpdatePermissionsReq(Set.of(CommunityPermission.BAN_MEMBER));
        CommunityModerator mod = new CommunityModerator(member, community, Set.of());
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityModeratorRepository.findByUserIdAndCommunityId(member.getId(), 10L))
                .thenReturn(Optional.of(mod));

        communityService.updateModeratorPermissions(10L, member.getId(), req);

        verify(communityAuthService).requireOwner(owner.getId(), 10L);
        verify(communityModeratorRepository).save(mod);
        assertThat(mod.getPermissions()).contains(CommunityPermission.BAN_MEMBER);
    }

    // Rejects permission updates for missing moderators.
    @Test
    void updateModeratorPermissions_missingModerator_throwsNotFound() {
        CommunityReqDto.UpdatePermissionsReq req = new CommunityReqDto.UpdatePermissionsReq(Set.of(CommunityPermission.BAN_MEMBER));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityModeratorRepository.findByUserIdAndCommunityId(member.getId(), 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.updateModeratorPermissions(10L, member.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Transfers ownership when the target is a member or moderator.
    @Test
    void transferOwnership_success_updatesOwner() {
        CommunityReqDto.TransferOwnershipReq req = new CommunityReqDto.TransferOwnershipReq(member.getId());
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        CommunityModerator mod = new CommunityModerator(member, community, Set.of());
        when(communityModeratorRepository.findByUserIdAndCommunityId(member.getId(), 10L))
                .thenReturn(Optional.of(mod));

        communityService.transferOwnership(10L, req);

        verify(communityAuthService).requireOwner(owner.getId(), 10L);
        assertThat(community.getOwner()).isEqualTo(member);
        verify(communityModeratorRepository).delete(mod);
        verify(notificationService).createNotification(eq(member.getId()), eq("OWNERSHIP"), any(), isNull(), eq(10L));
    }

    // Rejects ownership transfer to non-members/non-moderators.
    @Test
    void transferOwnership_requiresMembershipOrModerator() {
        User outsider = buildUser(9L, "outsider", "Out", "Side");
        CommunityReqDto.TransferOwnershipReq req = new CommunityReqDto.TransferOwnershipReq(outsider.getId());
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));
        when(communityModeratorRepository.existsByUserIdAndCommunityId(outsider.getId(), 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.transferOwnership(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("member or moderator");
    }

    // Includes the owner first and applies pagination bounds.
    @Test
    void getMembers_ownerFirst_withPagination() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(communityModeratorRepository.existsByUserIdAndCommunityId(anyLong(), eq(10L))).thenReturn(false);
        when(communityWarningRepository.countByUserIdAndCommunityId(anyLong(), eq(10L))).thenReturn(0L);

        Page<CommunityMemberResDto> result = communityService.getMembers(10L, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).userId()).isEqualTo(owner.getId());
    }

    // Returns empty pages when the offset exceeds member list size.
    @Test
    void getMembers_offsetBeyondSize_returnsEmpty() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(communityModeratorRepository.existsByUserIdAndCommunityId(anyLong(), eq(10L))).thenReturn(false);
        when(communityWarningRepository.countByUserIdAndCommunityId(anyLong(), eq(10L))).thenReturn(0L);

        Page<CommunityMemberResDto> result = communityService.getMembers(10L, PageRequest.of(5, 2));

        assertThat(result.getContent()).isEmpty();
    }

    // Builds a public listing without permission checks.
    @Test
    void getMembersPublic_returnsPagedResults() {
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(communityModeratorRepository.existsByUserIdAndCommunityId(anyLong(), eq(10L))).thenReturn(false);

        Page<CommunityMemberResDto> result = communityService.getMembersPublic(10L, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
    }

    // Builds a preview list of owner plus up to two members.
    @Test
    void getMembersPreview_returnsOwnerAndTwoMembers() {
        User extra = buildUser(3L, "extra", "Extra", "User");
        community.getMembers().add(extra);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(communityModeratorRepository.existsByUserIdAndCommunityId(anyLong(), eq(10L))).thenReturn(false);

        List<CommunityMemberResDto> result = communityService.getMembersPreview(10L);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).userId()).isEqualTo(owner.getId());
    }

    // Maps bans into public member DTOs with warning counts cleared.
    @Test
    void getBannedMembers_mapsFields() {
        CommunityBan ban = new CommunityBan(member, community, "spam");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityBanRepository.findByCommunityId(10L)).thenReturn(List.of(ban));

        List<CommunityMemberResDto> result = communityService.getBannedMembers(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(member.getId());
        assertThat(result.get(0).warningCount()).isZero();
    }

    // Aggregates bans and warnings into a moderation summary.
    @Test
    void getCommunityModeration_returnsBansAndWarnings() {
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        CommunityBan ban = new CommunityBan(member, community, "spam");
        ban.setId(1L);
        ban.setBannedAt(LocalDateTime.now());
        CommunityWarning warning = new CommunityWarning(member, community, "abuse");
        warning.setId(2L);
        warning.setWarnedAt(LocalDateTime.now());
        when(communityBanRepository.findByCommunityId(10L)).thenReturn(List.of(ban));
        when(communityWarningRepository.findByCommunityId(10L)).thenReturn(List.of(warning));

        CommunityModerationDto result = communityService.getCommunityModeration(10L);

        assertThat(result.getBans()).hasSize(1);
        assertThat(result.getWarnings()).hasSize(1);
    }

    // Prevents banning the community owner.
    @Test
    void banMember_owner_throwsForbidden() {
        CommunityReqDto.BanMemberReq req = new CommunityReqDto.BanMemberReq(owner.getId(), "reason");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> communityService.banMember(10L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    // Prevents duplicate bans and preserves data integrity.
    @Test
    void banMember_alreadyBanned_throwsIllegalState() {
        CommunityReqDto.BanMemberReq req = new CommunityReqDto.BanMemberReq(member.getId(), "reason");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(communityBanRepository.existsByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(true);

        assertThatThrownBy(() -> communityService.banMember(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already banned");
    }

    // Bans remove membership, decrement counts, and trigger notification.
    @Test
    void banMember_success_removesMemberAndSendsNotification() {
        CommunityReqDto.BanMemberReq req = new CommunityReqDto.BanMemberReq(member.getId(), "reason");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(communityBanRepository.existsByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(false);
        when(communityModeratorRepository.findByUserIdAndCommunityId(member.getId(), 10L))
                .thenReturn(Optional.empty());

        communityService.banMember(10L, req);

        assertThat(community.getMembers()).doesNotContain(member);
        assertThat(community.getNbrMembers()).isEqualTo(2);
        verify(communityBanRepository).save(any(CommunityBan.class));
        verify(notificationService).createNotification(eq(member.getId()), eq("BAN"), any(), isNull(), eq(10L));
    }

    // Unbans should fail if no ban exists for the user.
    @Test
    void unbanMember_missingBan_throwsNotFound() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityBanRepository.existsByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(false);

        assertThatThrownBy(() -> communityService.unbanMember(10L, member.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Unbans delete the existing ban record.
    @Test
    void unbanMember_success_deletesBan() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityBanRepository.existsByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(true);

        communityService.unbanMember(10L, member.getId());

        verify(communityBanRepository).deleteByUserIdAndCommunityId(member.getId(), 10L);
    }

    // Prevents warning the owner and enforces permissions.
    @Test
    void warnMember_owner_throwsForbidden() {
        CommunityReqDto.WarnMemberReq req = new CommunityReqDto.WarnMemberReq(owner.getId(), "reason");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> communityService.warnMember(10L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    // Triggers auto-ban after three warnings when user is not already banned.
    @Test
    void warnMember_threeWarnings_autoBans() {
        CommunityReqDto.WarnMemberReq req = new CommunityReqDto.WarnMemberReq(member.getId(), "reason");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(communityWarningRepository.countByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(3L);
        when(communityBanRepository.existsByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(false);
        when(communityModeratorRepository.findByUserIdAndCommunityId(member.getId(), 10L))
                .thenReturn(Optional.empty());

        communityService.warnMember(10L, req);

        verify(communityWarningRepository).save(any(CommunityWarning.class));
        verify(communityBanRepository).save(any(CommunityBan.class));
        verify(notificationService).createNotification(eq(member.getId()), eq("WARNING"), any(), isNull(), eq(10L));
    }

    // Issues a warning without auto-ban when the threshold is not reached.
    @Test
    void warnMember_underThreshold_doesNotAutoBan() {
        CommunityReqDto.WarnMemberReq req = new CommunityReqDto.WarnMemberReq(member.getId(), "reason");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(owner);
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(communityWarningRepository.countByUserIdAndCommunityId(member.getId(), 10L)).thenReturn(1L);

        communityService.warnMember(10L, req);

        verify(communityWarningRepository).save(any(CommunityWarning.class));
        verify(communityBanRepository, never()).save(any(CommunityBan.class));
    }

    // Returns the total number of communities in stats.
    @Test
    void getCommunityStats_returnsTotal() {
        when(communityRepository.count()).thenReturn(12L);

        var stats = communityService.getCommunityStats();

        assertThat(stats.get("total")).isEqualTo(12L);
    }

    // Returns mapped top communities ordered by member count.
    @Test
    void getTopCommunities_returnsMappedList() {
        when(communityRepository.findTopByMemberCount(PageRequest.of(0, 5)))
                .thenReturn(List.of(community));
        when(communityMapper.toDto(community)).thenReturn(new CommunityResDto(10L, "Title", "Desc", 3, owner.getId(), "cat", List.of()));

        List<CommunityResDto> result = communityService.getTopCommunities();

        assertThat(result).hasSize(1);
    }

    // Helper for consistent user setup across tests.
    private User buildUser(Long id, String username, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setJoinedCommunities(new ArrayList<>());
        user.setOwnedCommunities(new ArrayList<>());
        user.setBadges(new ArrayList<>());
        return user;
    }

    // Helper to keep community creation consistent in tests.
    private Community buildCommunity(Long id, User ownerUser, int members) {
        Community c = new Community();
        c.setId(id);
        c.setOwner(ownerUser);
        c.setNbrMembers(members);
        c.setMembers(new ArrayList<>());
        return c;
    }
}
