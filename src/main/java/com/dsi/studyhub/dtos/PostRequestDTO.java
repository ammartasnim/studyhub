package com.dsi.studyhub.dtos;

import com.dsi.studyhub.entities.PostStatus;

import java.util.List;

public class PostRequestDTO {
    private String title;
    private String content;
    private List<String> imgs;
    private Long authorId;
    private Long communityId;
}
