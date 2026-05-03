package com.dsi.studyhub.dtos;

import lombok.Builder;


import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
public record CommentResDto(
        Long id,
        Long postId,
        Long userId,
        String content,
        String authorFirstName,
        String authorLastName,
        String authorUsername,
        String authorPfp,
        LocalDateTime createdAt,
        boolean isLiked,
        int likeCount,
        Long parentCommentId,
        int replyCount

) implements Serializable {
}
