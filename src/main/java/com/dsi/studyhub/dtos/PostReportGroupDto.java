package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.PostStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Getter
@Setter
public class PostReportGroupDto {

    private Long postId;
    private String title;
    private String authorUsername;
    private String communityTitle;

    private long totalReports;
    private long approvedReports;

    private PostStatus status;

    private Map<String, Long> reasons;

    private LocalDateTime latestReportDate;
    private boolean hasPendingReports;
}