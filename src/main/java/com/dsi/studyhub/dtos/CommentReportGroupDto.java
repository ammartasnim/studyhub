package com.dsi.studyhub.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class CommentReportGroupDto {
    private Long commentId;
    private String content;
    private String authorUsername;
    private String postTitle;
    private Long totalReports;
    private Long approvedReports;
    private boolean hasPendingReports;
    private String status;
    private LocalDateTime latestReportDate;
    private Map<String, Long> reasons;
}