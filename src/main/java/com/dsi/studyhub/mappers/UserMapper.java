package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.UserReqDto;
import com.dsi.studyhub.dtos.UserResDto;
import com.dsi.studyhub.entities.User;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    User toEntity(UserReqDto dto);

    UserResDto toDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    void partialUpdate(UserReqDto dto, @MappingTarget User user);
}
