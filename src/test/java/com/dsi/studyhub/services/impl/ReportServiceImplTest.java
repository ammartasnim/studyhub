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
import com.dsi.studyhub.enums.ReportReason;
import com.dsi.studyhub.enums.ReportStatus;
import com.dsi.studyhub.enums.ReportTargetType;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.CommentRepository;
import com.dsi.studyhub.repositories.PostRepository;
import com.dsi.studyhub.repositories.ReportRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ReportRepository reportRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User reporter;
    private User author;
    private Post post;
    private Comment comment;
    private Report report;

    // Builds shared fixtures to keep each test focused on one behavior.
    @BeforeEach
    void setUp() throws Exception {
        reporter = buildUser(1L, "reporter");
        author = buildUser(2L, "author");

        post = new Post();
        post.setId(10L);
        post.setTitle("Post Title");
        post.setUser(author);
        post.setStatus(PostStatus.Approved);

        comment = new Comment();
        comment.setId(20L);
        comment.setContent("Comment content");
        comment.setUser(author);
        comment.setPost(post);
        comment.setStatus(CommentStatus.Active);

        report = new Report();
        report.setId(100L);
        report.setReporter(reporter);
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(post.getId());
        report.setReason(ReportReason.SPAM);
        report.setStatus(ReportStatus.PENDING);

        setFlagThreshold(2);
    }

    // Prevents users from reporting their own posts.
    @Test
    void reportPost_ownPost_throwsForbidden() {
        post.setUser(reporter);
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> reportService.reportPost(post.getId(), req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own post");
    }

    // Prevents duplicate reports by the same user.
    @Test
    void reportPost_duplicateReport_throwsForbidden() {
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reportRepository.existsByReporterAndTarget(reporter.getId(), ReportTargetType.POST, post.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.reportPost(post.getId(), req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("already reported");
    }

    // Rejects reports for missing posts.
    @Test
    void reportPost_postNotFound_throwsNotFound() {
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportPost(post.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    // Creates reports and includes post title preview in the response.
    @Test
    void reportPost_success_returnsDto() {
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, "context");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reportRepository.existsByReporterAndTarget(reporter.getId(), ReportTargetType.POST, post.getId()))
                .thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        ReportResDto result = reportService.reportPost(post.getId(), req);

        assertThat(result.getTargetPreview()).isEqualTo(post.getTitle());
        assertThat(result.getReason()).isEqualTo(ReportReason.SPAM);
    }

    // Prevents users from reporting their own comments.
    @Test
    void reportComment_ownComment_throwsForbidden() {
        comment.setUser(reporter);
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> reportService.reportComment(comment.getId(), req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own comment");
    }

    // Prevents duplicate comment reports by the same user.
    @Test
    void reportComment_duplicateReport_throwsForbidden() {
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(reportRepository.existsByReporterAndTarget(reporter.getId(), ReportTargetType.COMMENT, comment.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.reportComment(comment.getId(), req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("already reported");
    }

    // Rejects reports for missing comments.
    @Test
    void reportComment_commentNotFound_throwsNotFound() {
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportComment(comment.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");
    }

    // Truncates long comment previews when reporting.
    @Test
    void reportComment_truncatesPreview() {
        comment.setContent("a".repeat(100));
        ReportReqDto req = new ReportReqDto(ReportReason.SPAM, null);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(reportRepository.existsByReporterAndTarget(reporter.getId(), ReportTargetType.COMMENT, comment.getId()))
                .thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        ReportResDto result = reportService.reportComment(comment.getId(), req);

        assertThat(result.getTargetPreview()).endsWith("...");
    }

    // Approving a report can flag a post when the threshold is reached.
    @Test
    void approveReport_flagsPostWhenThresholdReached() {
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(post.getId());
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.countApprovedReports(ReportTargetType.POST, post.getId())).thenReturn(2L);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        ReportResDto result = reportService.approveReport(report.getId());

        assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThat(post.getStatus()).isEqualTo(PostStatus.Flagged);
        assertThat(post.getFlaggedAt()).isNotNull();
        assertThat(result.getTargetPreview()).isEqualTo(post.getTitle());
    }

    // Rejecting a report can unflag a post below threshold.
    @Test
    void rejectReport_unflagsPostWhenBelowThreshold() {
        post.setStatus(PostStatus.Flagged);
        post.setFlaggedAt(LocalDateTime.now().minusDays(1));
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(post.getId());
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.countApprovedReports(ReportTargetType.POST, post.getId())).thenReturn(0L);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        reportService.rejectReport(report.getId());

        assertThat(post.getStatus()).isEqualTo(PostStatus.Approved);
        assertThat(post.getFlaggedAt()).isNull();
    }

    // Approving a report can flag a comment when the threshold is reached.
    @Test
    void approveReport_flagsCommentWhenThresholdReached() {
        report.setTargetType(ReportTargetType.COMMENT);
        report.setTargetId(comment.getId());
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.countApprovedReports(ReportTargetType.COMMENT, comment.getId())).thenReturn(2L);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        reportService.approveReport(report.getId());

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.Flagged);
        assertThat(comment.getFlaggedAt()).isNotNull();
    }

    // Rejecting a report can unflag comments below the threshold.
    @Test
    void rejectReport_unflagsCommentWhenBelowThreshold() {
        comment.setStatus(CommentStatus.Flagged);
        comment.setFlaggedAt(LocalDateTime.now().minusDays(1));
        report.setTargetType(ReportTargetType.COMMENT);
        report.setTargetId(comment.getId());
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.countApprovedReports(ReportTargetType.COMMENT, comment.getId())).thenReturn(0L);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        reportService.rejectReport(report.getId());

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.Active);
        assertThat(comment.getFlaggedAt()).isNull();
    }

    // Filters reports by status when the filter is provided.
    @Test
    void getAllReports_withStatus_filters() {
        Pageable pageable = PageRequest.of(0, 5);
        when(reportRepository.findByStatus(ReportStatus.APPROVED, pageable))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        Page<ReportResDto> result = reportService.getAllReports("approved", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Returns all reports when no status filter is provided.
    @Test
    void getAllReports_withoutStatus_returnsAll() {
        Pageable pageable = PageRequest.of(0, 5);
        when(reportRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        Page<ReportResDto> result = reportService.getAllReports(null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Uses the authenticated reporter for personal report listings.
    @Test
    void getMyReports_usesAuthenticatedUser() {
        Pageable pageable = PageRequest.of(0, 5);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(reporter);
        when(reportRepository.findByReporter(reporter, pageable))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        Page<ReportResDto> result = reportService.getMyReports(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // Groups post reports and filters out deleted posts.
    @Test
    void getGroupedPostReports_filtersMissingPosts() {
        when(reportRepository.groupPostReports()).thenReturn(List.of(new Object[] {post.getId(), 2L, 1L}));
        when(postRepository.findById(post.getId())).thenReturn(Optional.empty());

        List<PostReportGroupDto> result = reportService.getGroupedPostReports();

        assertThat(result).isEmpty();
    }

    // Builds grouped post report summaries with reason breakdowns.
    @Test
    void getGroupedPostReports_buildsSummary() {
        when(reportRepository.groupPostReports()).thenReturn(List.of(new Object[] {post.getId(), 2L, 1L}));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus(ReportTargetType.POST, post.getId(), ReportStatus.PENDING))
                .thenReturn(1L);
        when(reportRepository.findTopByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.POST, post.getId()))
                .thenReturn(Optional.of(report));
        when(reportRepository.findByTargetTypeAndTargetId(ReportTargetType.POST, post.getId()))
                .thenReturn(List.of(report));

        List<PostReportGroupDto> result = reportService.getGroupedPostReports();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).hasPendingReports()).isTrue();
    }

    // Groups comment reports and filters out deleted comments.
    @Test
    void getGroupedCommentReports_filtersMissingComments() {
        when(reportRepository.groupCommentReports()).thenReturn(List.of(new Object[] {comment.getId(), 2L, 1L}));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());

        List<CommentReportGroupDto> result = reportService.getGroupedCommentReports();

        assertThat(result).isEmpty();
    }

    // Builds grouped comment report summaries with previews and reason breakdowns.
    @Test
    void getGroupedCommentReports_buildsSummary() {
        when(reportRepository.groupCommentReports()).thenReturn(List.of(new Object[] {comment.getId(), 2L, 1L}));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus(ReportTargetType.COMMENT, comment.getId(), ReportStatus.PENDING))
                .thenReturn(1L);
        when(reportRepository.findTopByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType.COMMENT, comment.getId()))
                .thenReturn(Optional.of(report));
        when(reportRepository.findByTargetTypeAndTargetId(ReportTargetType.COMMENT, comment.getId()))
                .thenReturn(List.of(report));

        List<CommentReportGroupDto> result = reportService.getGroupedCommentReports();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).hasPendingReports()).isTrue();
    }

    // Returns reports for a specific post with resolved previews.
    @Test
    void getReportsForPost_returnsDtos() {
        when(reportRepository.findByTargetTypeAndTargetId(ReportTargetType.POST, post.getId()))
                .thenReturn(List.of(report));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        List<ReportResDto> result = reportService.getReportsForPost(post.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetPreview()).isEqualTo(post.getTitle());
    }

    // Returns reports for a specific comment with resolved previews.
    @Test
    void getReportsForComment_returnsDtos() {
        report.setTargetType(ReportTargetType.COMMENT);
        report.setTargetId(comment.getId());
        when(reportRepository.findByTargetTypeAndTargetId(ReportTargetType.COMMENT, comment.getId()))
                .thenReturn(List.of(report));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        List<ReportResDto> result = reportService.getReportsForComment(comment.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetPreview()).isEqualTo(comment.getContent());
    }

    // Verifies deleted targets fall back to "Deleted post/comment" preview text.
    @Test
    void resolvePreview_deletedTarget_returnsFallback() {
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(post.getId());
        when(reportRepository.findByTargetTypeAndTargetId(ReportTargetType.POST, post.getId()))
                .thenReturn(List.of(report));
        when(postRepository.findById(post.getId())).thenReturn(Optional.empty());

        List<ReportResDto> result = reportService.getReportsForPost(post.getId());

        assertThat(result.get(0).getTargetPreview()).isEqualTo("Deleted post");
    }

    // Helper for consistent user setup across tests.
    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    // Injects a lower flag threshold to make flagging tests deterministic.
    private void setFlagThreshold(int value) throws Exception {
        Field field = ReportServiceImpl.class.getDeclaredField("flagThreshold");
        field.setAccessible(true);
        field.setInt(reportService, value);
    }
}
