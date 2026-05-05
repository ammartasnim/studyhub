package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.ReportReqDto;
import com.dsi.studyhub.dtos.ReportResDto;
import com.dsi.studyhub.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ─── SUBMIT REPORTS (any authenticated user) ──────────────────────────────

    @PostMapping("/post/{postId}")
    public ResponseEntity<ReportResDto> reportPost(
            @PathVariable Long postId,
            @RequestBody ReportReqDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.reportPost(postId, request));
    }

    @PostMapping("/comment/{commentId}")
    public ResponseEntity<ReportResDto> reportComment(
            @PathVariable Long commentId,
            @RequestBody ReportReqDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.reportComment(commentId, request));
    }

    // ─── ADMIN ONLY ───────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Page<ReportResDto>> getAllReports(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(reportService.getAllReports(status, pageable));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ReportResDto> approveReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.approveReport(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ReportResDto> rejectReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.rejectReport(id));
    }
}