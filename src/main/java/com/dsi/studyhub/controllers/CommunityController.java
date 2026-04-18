package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.CommunityRequestDTO;
import com.dsi.studyhub.dtos.CommunityResponseDTO;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.mappers.CommunityMapper;
import com.dsi.studyhub.services.CommunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<CommunityResponseDTO> createCommunity(@Valid @RequestBody CommunityRequestDTO requestDTO) {
        Community savedCommunity = communityService.createCommunity(requestDTO);
        return ResponseEntity.ok(communityMapper.toResponseDTO(savedCommunity));
    }

    @GetMapping
    public ResponseEntity<Page<CommunityResponseDTO>> getAllCommunities(
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

        Page<CommunityResponseDTO> communities = communityService
                .getAllCommunities(title, description, minMembers, pageable)
                .map(communityMapper::toResponseDTO);

        return ResponseEntity.ok(communities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponseDTO> getCommunityById(@PathVariable Long id) {
        return communityService.getCommunityById(id)
                .map(communityMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityResponseDTO> updateCommunity(@PathVariable Long id,
                                                                 @Valid @RequestBody CommunityRequestDTO requestDTO) {
        Community updatedCommunity = communityService.updateCommunity(id, communityMapper.toEntity(requestDTO));
        return ResponseEntity.ok(communityMapper.toResponseDTO(updatedCommunity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommunity(@PathVariable Long id) {
        communityService.deleteCommunity(id);
        return ResponseEntity.noContent().build();
    }
}

