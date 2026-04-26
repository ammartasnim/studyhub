package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.BadgeReqDto;
import com.dsi.studyhub.dtos.BadgeResDto;
import com.dsi.studyhub.entities.Badge;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface BadgeMapper {
    @Mapping(source = "user.id", target = "userId")
    BadgeResDto toDto(Badge badge);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(BadgeReqDto dto, @MappingTarget Badge badge);
}
