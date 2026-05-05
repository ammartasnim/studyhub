package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.ReportReqDto;
import com.dsi.studyhub.dtos.ReportResDto;
import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.Report;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.PostStatus;
import com.dsi.studyhub.enums.ReportStatus;
import com.dsi.studyhub.enums.ReportTargetType;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.CommentRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.repositories.ReportRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.ReportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository    reportRepository;
    private final PostRepository      postRepository;
    private final CommentRepository   commentRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Value("${app.report.flag-threshold:3}")
    private int flagThreshold;

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReportResDto reportPost(Long postId, ReportReqDto request) {
        User reporter = authenticatedUserService.getAuthenticatedUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // Prevent self-reporting
        if (post.getUser().getId().equals(reporter.getId())) {
            throw new ForbiddenException("You cannot report your own post.");
        }

        // Prevent duplicate reports
        if (reportRepository.existsByReporterAndTarget(
                reporter.getId(), ReportTargetType.POST, postId)) {
            throw new ForbiddenException("You have already reported this post.");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(postId);
        report.setReason(request.reason());
        report.setAdditionalContext(request.additionalContext());

        return toDto(reportRepository.save(report), post.getTitle());
    }

    @Override
    @Transactional
    public ReportResDto reportComment(Long commentId, ReportReqDto request) {
        User reporter = authenticatedUserService.getAuthenticatedUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Prevent self-reporting
        if (comment.getUser().getId().equals(reporter.getId())) {
            throw new ForbiddenException("You cannot report your own comment.");
        }

        // Prevent duplicate reports
        if (reportRepository.existsByReporterAndTarget(
                reporter.getId(), ReportTargetType.COMMENT, commentId)) {
            throw new ForbiddenException("You have already reported this comment.");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(ReportTargetType.COMMENT);
        report.setTargetId(commentId);
        report.setReason(request.reason());
        report.setAdditionalContext(request.additionalContext());

        String preview = comment.getContent() != null && comment.getContent().length() > 80
                ? comment.getContent().substring(0, 80) + "..."
                : comment.getContent();

        return toDto(reportRepository.save(report), preview);
    }

    // ─── ADMIN ACTIONS ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReportResDto approveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(ReportStatus.APPROVED);
        reportRepository.save(report);

        // Check threshold and flag target if reached
        long approvedCount = reportRepository.countApprovedReports(
                report.getTargetType(), report.getTargetId());

        if (approvedCount >= flagThreshold) {
            flagTarget(report.getTargetType(), report.getTargetId());
        }

        String preview = resolvePreview(report.getTargetType(), report.getTargetId());
        return toDto(report, preview);
    }

    @Override
    @Transactional
    public ReportResDto rejectReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(ReportStatus.REJECTED);
        reportRepository.save(report);

        String preview = resolvePreview(report.getTargetType(), report.getTargetId());
        return toDto(report, preview);
    }

    @Override
    @Transactional
    public Page<ReportResDto> getAllReports(String status, Pageable pageable) {
        Page<Report> reports;

        if (status != null && !status.isBlank()) {
            ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
            reports = reportRepository.findByStatus(reportStatus, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }

        return reports.map(r -> toDto(r, resolvePreview(r.getTargetType(), r.getTargetId())));
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private void flagTarget(ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.POST) {
            postRepository.findById(targetId).ifPresent(post -> {
                post.setStatus(PostStatus.Flagged);
                postRepository.save(post);
            });
        }
        // Comments don't have a status field yet — can be extended later
    }

    private String resolvePreview(ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.POST) {
            return postRepository.findById(targetId)
                    .map(Post::getTitle)
                    .orElse("Deleted post");
        } else {
            return commentRepository.findById(targetId)
                    .map(c -> {
                        String content = c.getContent();
                        return content != null && content.length() > 80
                                ? content.substring(0, 80) + "..."
                                : content;
                    })
                    .orElse("Deleted comment");
        }
    }

    private ReportResDto toDto(Report report, String targetPreview) {
        return ReportResDto.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterUsername(report.getReporter().getUsername())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetPreview(targetPreview)
                .reason(report.getReason())
                .status(report.getStatus())
                .additionalContext(report.getAdditionalContext())
                .createdAt(report.getCreatedAt())
                .build();
    }
    @Override
    @Transactional
    public Page<ReportResDto> getMyReports(Pageable pageable) {
        User reporter = authenticatedUserService.getAuthenticatedUser();
        return reportRepository.findByReporter(reporter, pageable)
                .map(r -> toDto(r, resolvePreview(r.getTargetType(), r.getTargetId())));
    }

}