package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.PostStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class PostResDto {
    private Long id;
    private String title;
    private String content;
    private List<String> imgs;
    private String userUsername;
    private String userFirstName;
    private String userLastName;
    private String userPfp;
    private String communityTitle;
    private PostStatus status;
    private int likeCount;
    private int commentCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
}