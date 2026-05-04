package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.entities.Community;
import com.dsi.studyhub.entities.CommunityModerator;
import com.dsi.studyhub.entities.User;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommunityMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "moderators", ignore = true)
    Community toEntity(CommunityReqDto dto);

    @Mapping(source = "owner", target = "ownerId", qualifiedByName = "userToId")
    @Mapping(source = "moderators", target = "moderators", qualifiedByName = "mappedModerators")
    @Mapping(source = "community", target = "nbrMembers", qualifiedByName = "computeMemberCount")
    CommunityResDto toDto(Community community);

    @Named("computeMemberCount")
    default int computeMemberCount(Community community) {
        if (community == null) return 0;
        long regularMembers = community.getMembers() == null ? 0 :
                community.getMembers().stream()
                        .filter(m -> !m.getId().equals(community.getOwner().getId()))
                        .count();
        return (int) regularMembers + 1;
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "moderators", ignore = true)
    void partialUpdate(CommunityReqDto dto, @MappingTarget Community community);

    @Named("userToId")
    default Long userToId(User user) {
        return user != null ? user.getId() : null;
    }

    @Named("mappedModerators")
    default List<CommunityResDto.ModeratorInfo> mappedModerators(List<CommunityModerator> moderators) {
        if (moderators == null) return List.of();
        return moderators.stream()
                .map(m -> new CommunityResDto.ModeratorInfo(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getUser().getFirstName() + " " + m.getUser().getLastName(),
                        m.getPermissions()
                ))
                .collect(Collectors.toList());
    }


}