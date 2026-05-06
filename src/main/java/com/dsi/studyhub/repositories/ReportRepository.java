package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Report;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.ReportStatus;
import com.dsi.studyhub.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // Check if user already reported this target
    Optional<Report> findByReporterIdAndTargetTypeAndTargetId(
            Long reporterId, ReportTargetType targetType, Long targetId);

    // Get all reports filtered by status (admin)
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    // Get all reports regardless of status (admin)
    Page<Report> findAll(Pageable pageable);

    // Count approved reports for a specific target
    @Query("""
        SELECT COUNT(r) FROM Report r
        WHERE r.targetType = :targetType
        AND r.targetId = :targetId
        AND r.status = com.dsi.studyhub.enums.ReportStatus.APPROVED
    """)
    long countApprovedReports(
            @Param("targetType") ReportTargetType targetType,
            @Param("targetId") Long targetId);

    // Check if a user has already reported a specific target
    @Query("""
        SELECT COUNT(r) > 0 FROM Report r
        WHERE r.reporter.id = :reporterId
        AND r.targetType = :targetType
        AND r.targetId = :targetId
    """)
    boolean existsByReporterAndTarget(
            @Param("reporterId") Long reporterId,
            @Param("targetType") ReportTargetType targetType,
            @Param("targetId") Long targetId);
    Page<Report> findByReporter(User reporter, Pageable pageable);
    //refine
    @Query("""
SELECT r.targetId as postId,
       COUNT(r) as totalReports,
       SUM(CASE WHEN r.status = 'APPROVED' THEN 1 ELSE 0 END) as approvedReports
FROM Report r
WHERE r.targetType = 'POST'
GROUP BY r.targetId
""")
    List<Object[]> groupPostReports();
    Optional<Report> findTopByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType,
            Long targetId
    );
    List<Report> findByTargetTypeAndTargetId(
            ReportTargetType targetType,
            Long targetId
    );
    long countByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType, Long targetId, ReportStatus status);
    @Query("""
    SELECT r.targetId, COUNT(r), SUM(CASE WHEN r.status = 'APPROVED' THEN 1 ELSE 0 END)
    FROM Report r
    WHERE r.targetType = 'COMMENT'
    GROUP BY r.targetId
""")
    List<Object[]> groupCommentReports();
}