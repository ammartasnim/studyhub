package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.ReportReason;

public record ReportReqDto(
        ReportReason reason,
        String additionalContext
) {}