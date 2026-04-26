package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.entities.Post;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PostMapper {
    Post toEntity(PostReqDto postReqDto);

    PostResDto toDto(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PostReqDto postReqDto, @MappingTarget Post post);
}