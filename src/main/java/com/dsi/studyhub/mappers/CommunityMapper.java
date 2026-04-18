package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.CommunityRequestDTO;
import com.dsi.studyhub.dtos.CommunityResponseDTO;
import com.dsi.studyhub.entities.Community;
import org.springframework.stereotype.Component;

@Component
public class CommunityMapper {

    public Community toEntity(CommunityRequestDTO dto) {
        Community community = new Community();
        community.setTitle(dto.getTitle());
        community.setDescription(dto.getDescription());
        community.setNbrMembers(dto.getNbrMembers());
        return community;
    }

    public CommunityResponseDTO toResponseDTO(Community community) {
        return new CommunityResponseDTO(
                community.getId(),
                community.getTitle(),
                community.getDescription(),
                community.getNbrMembers()
        );
    }

    public void updateEntity(Community community, CommunityRequestDTO dto) {
        if (dto.getTitle() != null) {
            community.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            community.setDescription(dto.getDescription());
        }
        if (dto.getNbrMembers() > 0) {
            community.setNbrMembers(dto.getNbrMembers());
        }
    }
}

