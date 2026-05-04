package com.dsi.studyhub.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "community_warnings",
        indexes = {
                @Index(name = "idx_community_warning_user", columnList = "user_id"),
                @Index(name = "idx_community_warning_community", columnList = "community_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CommunityWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime warnedAt;

    public CommunityWarning(User user, Community community, String reason) {
        this.user = user;
        this.community = community;
        this.reason = reason;
    }
}