package com.dsi.studyhub.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "community_bans",
        indexes = {
                @Index(name = "idx_community_ban_user", columnList = "user_id"),
                @Index(name = "idx_community_ban_community", columnList = "community_id"),
                @Index(name = "idx_community_ban_unique", columnList = "user_id, community_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CommunityBan {

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
    private LocalDateTime bannedAt;

    public CommunityBan(User user, Community community, String reason) {
        this.user = user;
        this.community = community;
        this.reason = reason;
    }
}