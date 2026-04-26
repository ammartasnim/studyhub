package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.CommunityReqDto;
import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.entities.Community;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommunityMapper {

    Community toEntity(CommunityReqDto dto);

    @Mapping(source = "moderator.id", target = "moderatorId")
    CommunityResDto toDto(Community community);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(CommunityReqDto dto, @MappingTarget Community community);
}
