package com.dsi.studyhub.dtos;

import com.dsi.studyhub.entities.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponseDTO {
    private Long id;
    private String title;
    private String content;
    private List<String> imgs;
    private String authorName;
    private String communityName;
    private String status;
    private int commentCount;
    private LocalDateTime createdAt;

}
