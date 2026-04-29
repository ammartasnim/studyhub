package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.PostStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record PostResDto(
        Long id,
        String title,
        String content,
        List<String> imgs,
        String userUsername,
        String userFirstName,
        String userLastName,
        String communityTitle,
        PostStatus status,
        int likeCount,
        int commentCount,
        boolean isLiked,
        LocalDateTime createdAt
) implements Serializable {}