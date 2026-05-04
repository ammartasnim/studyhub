package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.CommunityMemberResDto;
import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.mappers.CommunityMapper;
import com.dsi.studyhub.services.CommunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    private final CommunityService communityService;
    private final CommunityMapper communityMapper;

    public CommunityController(CommunityService communityService, CommunityMapper communityMapper) {
        this.communityService = communityService;
        this.communityMapper = communityMapper;
    }

    @PostMapping
    public ResponseEntity<CommunityResDto> createCommunity(@Valid @RequestBody CommunityReqDto requestDTO) {
        Community savedCommunity = communityService.createCommunity(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(communityMapper.toDto(savedCommunity));
    }

    @GetMapping
    public ResponseEntity<Page<CommunityResDto>> getAllCommunities(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer minMembers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(communityService.getAllCommunities(title, description, minMembers, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<CommunityResDto>> getMyCommunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(communityService.getMyCommunities(pageable));
    }

    @GetMapping("/my-created")
    public ResponseEntity<Page<CommunityResDto>> getMyCreatedCommunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(communityService.getMyCreatedCommunities(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResDto> getCommunityById(@PathVariable Long id) {
        return communityService.getCommunityById(id)
                .map(communityMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityResDto> updateCommunity(
            @PathVariable Long id,
            @Valid @RequestBody CommunityReqDto requestDTO) {
        Community updatedCommunity = communityService.updateCommunity(id, requestDTO);
        return ResponseEntity.ok(communityMapper.toDto(updatedCommunity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommunity(@PathVariable Long id) {
        communityService.deleteCommunity(id);
        return ResponseEntity.noContent().build();
    }

    // Moderator management
    @PostMapping("/{id}/moderators")
    public ResponseEntity<Void> addModerator(
            @PathVariable Long id,
            @Valid @RequestBody CommunityReqDto.AddModeratorReq request) {
        communityService.addModerator(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/moderators/{userId}")
    public ResponseEntity<Void> removeModerator(
            @PathVariable Long id,
            @PathVariable Long userId) {
        communityService.removeModerator(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/moderators/{userId}/permissions")
    public ResponseEntity<Void> updateModeratorPermissions(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody CommunityReqDto.UpdatePermissionsReq request) {
        communityService.updateModeratorPermissions(id, userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/transfer-ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long id,
            @Valid @RequestBody CommunityReqDto.TransferOwnershipReq request) {
        communityService.transferOwnership(id, request);
        return ResponseEntity.ok().build();
    }

    // Member management
    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinCommunity(@PathVariable Long id) {
        communityService.joinCommunity(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leaveCommunity(@PathVariable Long id) {
        communityService.leaveCommunity(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getCommunityStats() {
        return ResponseEntity.ok(communityService.getCommunityStats());
    }

    @GetMapping("/stats/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommunityResDto>> getTopCommunities() {
        return ResponseEntity.ok(communityService.getTopCommunities());
    }
    @GetMapping("/{id}/members")
    public ResponseEntity<Page<CommunityMemberResDto>> getMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(communityService.getMembers(id, pageable));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<Void> banMember(
            @PathVariable Long id,
            @RequestBody CommunityReqDto.BanMemberReq request) {
        communityService.banMember(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/ban/{userId}")
    public ResponseEntity<Void> unbanMember(
            @PathVariable Long id,
            @PathVariable Long userId) {
        communityService.unbanMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/warn")
    public ResponseEntity<Void> warnMember(
            @PathVariable Long id,
            @RequestBody CommunityReqDto.WarnMemberReq request) {
        communityService.warnMember(id, request);
        return ResponseEntity.ok().build();
    }
}