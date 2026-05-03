package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.PostReqDto;
import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.services.AuthenticatedUserService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)

public abstract class PostMapper {

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Mapping(target = "imgs", ignore = true)  // ✅ service handles this
    public abstract Post toEntity(PostReqDto postReqDto);

    @Mapping(target = "userUsername",  source = "user.username")
    @Mapping(target = "userFirstName", source = "user.firstName")
    @Mapping(target = "userLastName",  source = "user.lastName")
    @Mapping(target = "userPfp",       source = "user.pfp")
    @Mapping(target = "communityTitle", source = "community.title")
    @Mapping(target = "likeCount",    ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "isLiked",      ignore = true)
    public abstract PostResDto toDto(Post post);

    @AfterMapping
    protected void computeExtraFields(Post post, @MappingTarget PostResDto.PostResDtoBuilder builder) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();

        builder.likeCount(post.getLikes() == null ? 0 : post.getLikes().size());
        builder.commentCount(post.getComments() == null ? 0 : post.getComments().size());
        builder.isLiked(post.getLikes() != null && post.getLikes().stream()
                .anyMatch(u -> u.getId().equals(currentUser.getId())));
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "imgs", ignore = true)
    public abstract Post partialUpdate(PostReqDto postReqDto, @MappingTarget Post post);
}