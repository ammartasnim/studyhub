package com.dsi.studyhub.mappers;

import com.dsi.studyhub.dtos.CommentReqDto;
import com.dsi.studyhub.dtos.CommentResDto;
import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.services.AuthenticatedUserService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class CommentMapper {
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Mapping(source = "post.id", target = "postId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.firstName", target = "authorFirstName")
    @Mapping(source = "user.lastName", target = "authorLastName")
    @Mapping(source = "user.username", target = "authorUsername")
    @Mapping(source = "user.pfp", target = "authorPfp")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "parentComment.id", target = "parentCommentId")
    @Mapping(target = "isLiked",      ignore = true)
    @Mapping(expression = "java(comment.getReplies().size())", target = "replyCount")

    public abstract CommentResDto toDto(Comment comment);

    @AfterMapping
    protected void computeExtraFields(Comment comment, @MappingTarget CommentResDto.CommentResDtoBuilder builder) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();

        builder.likeCount(comment.getLikedByUsers() == null ? 0 : comment.getLikedByUsers().size());
        builder.replyCount(comment.getReplies() == null ? 0 : comment.getReplies().size());
        builder.isLiked(comment.getLikedByUsers() != null && comment.getLikedByUsers().stream()
                .anyMatch(u -> u.getId().equals(currentUser.getId())));
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void partialUpdate(CommentReqDto dto, @MappingTarget Comment comment);
}
