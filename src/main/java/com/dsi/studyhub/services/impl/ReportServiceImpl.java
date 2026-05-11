package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.CommentReportGroupDto;
import com.dsi.studyhub.dtos.PostReportGroupDto;
import com.dsi.studyhub.dtos.ReportReqDto;
import com.dsi.studyhub.dtos.ReportResDto;
import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.Report;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.CommentStatus;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository         reportRepository;
    private final PostRepository           postRepository;
    private final CommentRepository        commentRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Value("${app.report.flag-threshold:5}")
    private int flagThreshold;


    // Report submission
    @Override
    @Transactional
    public ReportResDto reportPost(Long postId, ReportReqDto request) {
        User reporter = authenticatedUserService.getAuthenticatedUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getUser().getId().equals(reporter.getId()))
            throw new ForbiddenException("You cannot report your own post.");

        if (reportRepository.existsByReporterAndTarget(
                reporter.getId(), ReportTargetType.POST, postId))
            throw new ForbiddenException("You have already reported this post.");

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

        if (comment.getUser().getId().equals(reporter.getId()))
            throw new ForbiddenException("You cannot report your own comment.");

        if (reportRepository.existsByReporterAndTarget(
                reporter.getId(), ReportTargetType.COMMENT, commentId))
            throw new ForbiddenException("You have already reported this comment.");

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

    // Admin actions
    @Override
    @Transactional
    public ReportResDto approveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(ReportStatus.APPROVED);
        reportRepository.save(report);
        updateTargetStatus(report.getTargetType(), report.getTargetId());

        return toDto(report, resolvePreview(report.getTargetType(), report.getTargetId()));
    }

    @Override
    @Transactional
    public ReportResDto rejectReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(ReportStatus.REJECTED);
        reportRepository.save(report);
        updateTargetStatus(report.getTargetType(), report.getTargetId());

        return toDto(report, resolvePreview(report.getTargetType(), report.getTargetId()));
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

    @Override
    @Transactional
    public Page<ReportResDto> getMyReports(Pageable pageable) {
        User reporter = authenticatedUserService.getAuthenticatedUser();
        return reportRepository.findByReporter(reporter, pageable)
                .map(r -> toDto(r, resolvePreview(r.getTargetType(), r.getTargetId())));
    }

    // Grouped post reports
    @Override
    @Transactional
    public List<PostReportGroupDto> getGroupedPostReports() {
        List<Object[]> results = reportRepository.groupPostReports();

        return results.stream().map(row -> {
            Long postId          = (Long) row[0];
            Long totalReports    = (Long) row[1];
            Long approvedReports = (Long) row[2];

            Post post = postRepository.findById(postId).orElse(null);
            if (post == null) return null;

            long pendingCount = reportRepository.countByTargetTypeAndTargetIdAndStatus(
                    ReportTargetType.POST, postId, ReportStatus.PENDING);

            return PostReportGroupDto.builder()
                    .postId(postId)
                    .title(post.getTitle())
                    .authorUsername(post.getUser().getUsername())
                    .communityTitle(post.getCommunity() != null ? post.getCommunity().getTitle() : null)
                    .totalReports(totalReports)
                    .approvedReports(approvedReports)
                    .hasPendingReports(pendingCount > 0)
                    .status(post.getStatus())
                    .latestReportDate(getLatestReportDate(postId, ReportTargetType.POST))
                    .reasons(getReasonBreakdown(postId, ReportTargetType.POST))
                    .content(post.getContent())
                    .imgs(post.getImgs() != null ? post.getImgs() : java.util.List.of())
                    .postCreatedAt(post.getCreatedAt())
                    .userPfp(post.getUser().getPfp())
                    .build();

        }).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional
    public List<ReportResDto> getReportsForPost(Long postId) {
        return reportRepository
                .findByTargetTypeAndTargetId(ReportTargetType.POST, postId)
                .stream()
                .map(r -> toDto(r, resolvePreview(r.getTargetType(), r.getTargetId())))
                .toList();
    }

    // Grouped comment reports
    @Override
    @Transactional
    public List<CommentReportGroupDto> getGroupedCommentReports() {
        List<Object[]> results = reportRepository.groupCommentReports();

        return results.stream().map(row -> {
            Long commentId       = (Long) row[0];
            Long totalReports    = (Long) row[1];
            Long approvedReports = (Long) row[2];

            Comment comment = commentRepository.findById(commentId).orElse(null);
            if (comment == null) return null;

            long pendingCount = reportRepository.countByTargetTypeAndTargetIdAndStatus(
                    ReportTargetType.COMMENT, commentId, ReportStatus.PENDING);

            String preview = comment.getContent() != null && comment.getContent().length() > 80
                    ? comment.getContent().substring(0, 80) + "..."
                    : comment.getContent();

            return CommentReportGroupDto.builder()
                    .commentId(commentId)
                    .content(preview)
                    .authorUsername(comment.getUser().getUsername())
                    .postTitle(comment.getPost().getTitle())
                    .totalReports(totalReports)
                    .approvedReports(approvedReports)
                    .hasPendingReports(pendingCount > 0)
                    .status(comment.getStatus().name())
                    .latestReportDate(getLatestReportDate(commentId, ReportTargetType.COMMENT))
                    .reasons(getReasonBreakdown(commentId, ReportTargetType.COMMENT))
                    .build();

        }).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional
    public List<ReportResDto> getReportsForComment(Long commentId) {
        return reportRepository
                .findByTargetTypeAndTargetId(ReportTargetType.COMMENT, commentId)
                .stream()
                .map(r -> toDto(r, resolvePreview(r.getTargetType(), r.getTargetId())))
                .toList();
    }

    // Reporting helpers
    private void updateTargetStatus(ReportTargetType type, Long targetId) {
        // Applies flagging/unflagging based on approved report threshold.
        long approvedCount = reportRepository.countApprovedReports(type, targetId);

        if (type == ReportTargetType.POST) {
            postRepository.findById(targetId).ifPresent(post -> {
                if (approvedCount >= flagThreshold) {
                    if (post.getStatus() != PostStatus.Flagged) {
                        post.setStatus(PostStatus.Flagged);
                        post.setFlaggedAt(LocalDateTime.now());
                    }
                } else {
                    if (post.getStatus() == PostStatus.Flagged) {
                        post.setStatus(PostStatus.Approved);
                        post.setFlaggedAt(null);
                    }
                }
                postRepository.save(post);
            });
        } else if (type == ReportTargetType.COMMENT) {
            commentRepository.findById(targetId).ifPresent(comment -> {
                if (approvedCount >= flagThreshold) {
                    if (comment.getStatus() != CommentStatus.Flagged) {
                        comment.setStatus(CommentStatus.Flagged);
                        comment.setFlaggedAt(LocalDateTime.now());
                    }
                } else {
                    if (comment.getStatus() == CommentStatus.Flagged) {
                        comment.setStatus(CommentStatus.Active);
                        comment.setFlaggedAt(null);
                    }
                }
                commentRepository.save(comment);
            });
        }
    }

    private LocalDateTime getLatestReportDate(Long targetId, ReportTargetType type) {
        return reportRepository
                .findTopByTargetTypeAndTargetIdOrderByCreatedAtDesc(type, targetId)
                .map(Report::getCreatedAt)
                .orElse(null);
    }

    private Map<String, Long> getReasonBreakdown(Long targetId, ReportTargetType type) {
        return reportRepository
                .findByTargetTypeAndTargetId(type, targetId)
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReason().name(),
                        Collectors.counting()
                ));
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
}
