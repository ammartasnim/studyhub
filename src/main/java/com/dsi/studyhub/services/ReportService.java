package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.ReportReqDto;
import com.dsi.studyhub.dtos.ReportResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {
    ReportResDto reportPost(Long postId, ReportReqDto request);
    ReportResDto reportComment(Long commentId, ReportReqDto request);
    ReportResDto approveReport(Long reportId);
    ReportResDto rejectReport(Long reportId);
    Page<ReportResDto> getAllReports(String status, Pageable pageable);
    Page<ReportResDto> getMyReports(Pageable pageable);

}