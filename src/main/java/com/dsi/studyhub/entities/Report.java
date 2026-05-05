package com.dsi.studyhub.entities;

import com.dsi.studyhub.enums.ReportReason;
import com.dsi.studyhub.enums.ReportStatus;
import com.dsi.studyhub.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reports",
        indexes = {
                @Index(name = "idx_report_reporter", columnList = "reporter_id"),
                @Index(name = "idx_report_target",   columnList = "target_type, target_id"),
                @Index(name = "idx_report_status",   columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_report_per_user_target",
                        columnNames = {"reporter_id", "target_type", "target_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(length = 500)
    private String additionalContext;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}