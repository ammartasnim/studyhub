package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommunityMemberResDto;
import com.dsi.studyhub.dtos.CommunityModerationDto;
import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.entities.*;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.enums.CommunityPermission;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.mappers.CommunityMapper;
import com.dsi.studyhub.repositories.*;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.CommunityAuthService;
import com.dsi.studyhub.services.CommunityService;
import com.dsi.studyhub.services.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMapper communityMapper;
    private final AuthenticatedUserService authenticatedUserService;

    @Autowired private CommunityBanRepository communityBanRepository;
    @Autowired private CommunityWarningRepository communityWarningRepository;
    @Autowired private CommunityAuthService communityAuthService;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityModeratorRepository communityModeratorRepository;
    @Autowired private NotificationService notificationService;

    public CommunityServiceImpl(CommunityRepository communityRepository,
                                CommunityMapper communityMapper,
                                AuthenticatedUserService authenticatedUserService) {
        this.communityRepository = communityRepository;
        this.communityMapper = communityMapper;
        this.authenticatedUserService = authenticatedUserService;
    }

    // Community creation and updates
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
        newCommunity.setOwner(moderator);
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
        return communityRepository.findByOwner(currentUser.getId(), pageable)
                .map(communityMapper::toDto);
    }

    @Override
    public Page<CommunityResDto> getAllCommunities(String title, String description, Integer minMembers, Pageable pageable) {
        return communityRepository.findByFilters(title, description, minMembers, pageable)
                .map(communityMapper::toDto);
    }

    @Override
    @Transactional
    public Community updateCommunity(Long id, CommunityReqDto communityDto) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + id));
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), id, CommunityPermission.EDIT_COMMUNITY);
        communityMapper.partialUpdate(communityDto, community);
        if (communityDto.nbrMembers() != null && communityDto.nbrMembers() <= 0) {
            community.setNbrMembers(1);
        }
        return communityRepository.save(community);
    }

    @Override
    @Transactional
    public void deleteCommunity(Long id) {
        communityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + id));
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwner(currentUser.getId(), id);
        communityRepository.deleteById(id);
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

    // Membership management
    @Override
    @Transactional
    public void joinCommunity(Long communityId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        if (communityBanRepository.existsByUserIdAndCommunityId(currentUser.getId(), communityId)) {
            throw new ForbiddenException("You are banned from this community.");
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        if (!community.getMembers().contains(currentUser)) {
            throw new IllegalStateException("You are not a member of this community.");
        }

        if (community.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are the owner — transfer or delete the community instead of leaving.");
        }

        community.getMembers().remove(currentUser);
        currentUser.getJoinedCommunities().remove(community);
        community.setNbrMembers(Math.max(1, community.getNbrMembers() - 1));
        communityRepository.save(community);
    }

    // Moderator management
    @Override
    @Transactional
    public void addModerator(Long communityId, CommunityReqDto.AddModeratorReq request) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.ADD_MODERATOR);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        User newMod = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

        if (community.getOwner().getId().equals(newMod.getId())) {
            throw new IllegalStateException("Owner cannot be added as a moderator.");
        }

        if (communityModeratorRepository.existsByUserIdAndCommunityId(newMod.getId(), communityId)) {
            throw new IllegalStateException("User is already a moderator of this community.");
        }

        CommunityModerator mod = new CommunityModerator(
                newMod, community,
                request.permissions() != null ? request.permissions() : Set.of()
        );
        communityModeratorRepository.save(mod);

        notificationService.createNotification(
                newMod.getId(),
                "MODERATOR",
                "You have been added as a moderator of the community: " + community.getTitle(),
                null,
                communityId
        );
    }

    @Override
    @Transactional
    public void removeModerator(Long communityId, Long userId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.REMOVE_MODERATOR);

        if (!communityModeratorRepository.existsByUserIdAndCommunityId(userId, communityId)) {
            throw new ResourceNotFoundException("Moderator not found in this community.");
        }

        communityModeratorRepository.deleteByCommunityIdAndUserId(communityId, userId);
    }

    @Override
    @Transactional
    public void updateModeratorPermissions(Long communityId, Long userId, CommunityReqDto.UpdatePermissionsReq request) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwner(currentUser.getId(), communityId);

        CommunityModerator mod = communityModeratorRepository
                .findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Moderator not found in this community."));

        mod.setPermissions(request.permissions());
        communityModeratorRepository.save(mod);
    }

    @Override
    @Transactional
    public void transferOwnership(Long communityId, CommunityReqDto.TransferOwnershipReq request) {
        // Transfers ownership and updates moderator roles if needed.
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwner(currentUser.getId(), communityId);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        User newOwner = userRepository.findById(request.newOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.newOwnerId()));

        if (!community.getMembers().contains(newOwner) &&
                !communityModeratorRepository.existsByUserIdAndCommunityId(newOwner.getId(), communityId)) {
            throw new IllegalStateException("New owner must be a member or moderator of the community.");
        }

        communityModeratorRepository.findByUserIdAndCommunityId(newOwner.getId(), communityId)
                .ifPresent(communityModeratorRepository::delete);

        community.setOwner(newOwner);
        communityRepository.save(community);

        notificationService.createNotification(
                newOwner.getId(),
                "OWNERSHIP",
                "You are now the owner of the community: " + community.getTitle(),
                null,
                communityId
        );
    }

    // Member listings
    @Override
    public Page<CommunityMemberResDto> getMembers(Long communityId, Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.VIEW_MEMBERS);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        User owner = community.getOwner();

        List<User> allMembers = new ArrayList<>();
        allMembers.add(owner);
        community.getMembers().stream()
                .filter(m -> !m.getId().equals(owner.getId()))
                .forEach(allMembers::add);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allMembers.size());

        if (start >= allMembers.size()) {
            return new PageImpl<>(List.of(), pageable, allMembers.size());
        }

        List<CommunityMemberResDto> dtos = allMembers.subList(start, end).stream()
                .map(m -> new CommunityMemberResDto(
                        m.getId(),
                        m.getUsername(),
                        m.getFirstName() + " " + m.getLastName(),
                        m.getPfp(),
                        m.getXpPts(),
                        m.getLevel(),
                        communityModeratorRepository.existsByUserIdAndCommunityId(m.getId(), communityId),
                        communityWarningRepository.countByUserIdAndCommunityId(m.getId(), communityId)
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, allMembers.size());
    }

    // Member moderation
    @Override
    @Transactional
    public void banMember(Long communityId, CommunityReqDto.BanMemberReq request) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.BAN_MEMBER);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        User target = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

        if (community.getOwner().getId().equals(target.getId())) {
            throw new ForbiddenException("Cannot ban the community owner.");
        }

        if (communityBanRepository.existsByUserIdAndCommunityId(target.getId(), communityId)) {
            throw new IllegalStateException("User is already banned from this community.");
        }

        community.getMembers().remove(target);
        target.getJoinedCommunities().remove(community);
        community.setNbrMembers(Math.max(0, community.getNbrMembers() - 1));
        communityRepository.save(community);

        communityModeratorRepository.findByUserIdAndCommunityId(target.getId(), communityId)
                .ifPresent(communityModeratorRepository::delete);

        communityBanRepository.save(new CommunityBan(target, community, request.reason()));

        notificationService.createNotification(
                target.getId(),
                "BAN",
                "You have been banned from the community: " + community.getTitle(),
                null,
                communityId
        );
    }

    @Override
    @Transactional
    public void unbanMember(Long communityId, Long userId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.BAN_MEMBER);

        if (!communityBanRepository.existsByUserIdAndCommunityId(userId, communityId)) {
            throw new ResourceNotFoundException("No ban found for this user in this community.");
        }

        communityBanRepository.deleteByUserIdAndCommunityId(userId, communityId);
    }

    @Override
    @Transactional
    public void warnMember(Long communityId, CommunityReqDto.WarnMemberReq request) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.WARN_MEMBER);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        User target = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

        if (community.getOwner().getId().equals(target.getId())) {
            throw new ForbiddenException("Cannot warn the community owner.");
        }

        communityWarningRepository.save(new CommunityWarning(target, community, request.reason()));

        notificationService.createNotification(
                target.getId(),
                "WARNING",
                "You have received a warning in the community: " + community.getTitle(),
                null,
                communityId
        );

        long warningCount = communityWarningRepository.countByUserIdAndCommunityId(target.getId(), communityId);
        if (warningCount >= 3 && !communityBanRepository.existsByUserIdAndCommunityId(target.getId(), communityId)) {
            banMember(communityId, new CommunityReqDto.BanMemberReq(target.getId(), "Auto-banned after 3 warnings"));
        }
    }
    @Override
    public List<CommunityMemberResDto> getBannedMembers(Long communityId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        communityAuthService.requireOwnerOrPermission(currentUser.getId(), communityId, CommunityPermission.BAN_MEMBER);
        return communityBanRepository.findByCommunityId(communityId).stream()
                .map(ban -> new CommunityMemberResDto(
                        ban.getUser().getId(),
                        ban.getUser().getUsername(),
                        ban.getUser().getFirstName() + " " + ban.getUser().getLastName(),
                        ban.getUser().getPfp(),
                        ban.getUser().getXpPts(),
                        ban.getUser().getLevel(),
                        false,
                        0
                ))
                .collect(Collectors.toList());
    }
    @Override
    public List<CommunityMemberResDto> getMembersPreview(Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        List<CommunityMemberResDto> result = new ArrayList<>();


        User owner = community.getOwner();
        result.add(new CommunityMemberResDto(
                owner.getId(),
                owner.getUsername(),
                owner.getFirstName() + " " + owner.getLastName(),
                owner.getPfp(),
                owner.getXpPts(),
                owner.getLevel(),
                false,
                0
        ));


        community.getMembers().stream()
                .filter(m -> !m.getId().equals(owner.getId()))
                .limit(2)
                .forEach(m -> result.add(new CommunityMemberResDto(
                        m.getId(),
                        m.getUsername(),
                        m.getFirstName() + " " + m.getLastName(),
                        m.getPfp(),
                        m.getXpPts(),
                        m.getLevel(),
                        communityModeratorRepository.existsByUserIdAndCommunityId(m.getId(), communityId),
                        0
                )));

        return result;
    }
    @Override
    public Page<CommunityMemberResDto> getMembersPublic(Long communityId, Pageable pageable) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        User owner = community.getOwner();
        List<User> allMembers = new ArrayList<>();
        allMembers.add(owner);
        community.getMembers().stream()
                .filter(m -> !m.getId().equals(owner.getId()))
                .forEach(allMembers::add);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allMembers.size());

        if (start >= allMembers.size()) {
            return new PageImpl<>(List.of(), pageable, allMembers.size());
        }

        List<CommunityMemberResDto> dtos = allMembers.subList(start, end).stream()
                .map(m -> new CommunityMemberResDto(
                        m.getId(),
                        m.getUsername(),
                        m.getFirstName() + " " + m.getLastName(),
                        m.getPfp(),
                        m.getXpPts(),
                        m.getLevel(),
                        communityModeratorRepository.existsByUserIdAndCommunityId(m.getId(), communityId),
                        0
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, allMembers.size());
    }
    @Override
    @Transactional
    public CommunityModerationDto getCommunityModeration(Long communityId) {
        List<CommunityModerationDto.BanEntry> bans = communityBanRepository
                .findByCommunityId(communityId)
                .stream()
                .map(b -> CommunityModerationDto.BanEntry.builder()
                        .id(b.getId())
                        .username(b.getUser().getUsername())
                        .firstName(b.getUser().getFirstName())
                        .lastName(b.getUser().getLastName())
                        .reason(b.getReason())
                        .bannedAt(b.getBannedAt())
                        .build())
                .toList();

        List<CommunityModerationDto.WarningEntry> warnings = communityWarningRepository
                .findByCommunityId(communityId)
                .stream()
                .map(w -> CommunityModerationDto.WarningEntry.builder()
                        .id(w.getId())
                        .username(w.getUser().getUsername())
                        .firstName(w.getUser().getFirstName())
                        .lastName(w.getUser().getLastName())
                        .reason(w.getReason())
                        .warnedAt(w.getWarnedAt())
                        .build())
                .toList();

        return CommunityModerationDto.builder()
                .bans(bans)
                .warnings(warnings)
                .build();
    }

    @Override
    public Page<CommunityResDto> getCommunitiesByUser(Long userId,  Pageable pageable) {
        return communityRepository.findByMembersId(userId, pageable)
                .map(communityMapper::toDto);
    }



}
