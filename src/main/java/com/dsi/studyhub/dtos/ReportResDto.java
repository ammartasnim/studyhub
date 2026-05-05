package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.ReportReason;
import com.dsi.studyhub.enums.ReportStatus;
import com.dsi.studyhub.enums.ReportTargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ReportResDto {
    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private ReportTargetType targetType;
    private Long targetId;
    private String targetPreview;
    private ReportReason reason;
    private ReportStatus status;
    private String additionalContext;
    private LocalDateTime createdAt;
}