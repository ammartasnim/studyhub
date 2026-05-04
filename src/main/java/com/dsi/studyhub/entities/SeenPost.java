package com.dsi.studyhub.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seen_posts",
        indexes = {
                @Index(name = "idx_seen_post_user", columnList = "user_id"),
                @Index(name = "idx_seen_post_post", columnList = "post_id"),
                @Index(name = "idx_seen_post_user_post", columnList = "user_id, post_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SeenPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime firstSeenAt;

    @UpdateTimestamp
    private LocalDateTime lastSeenAt;

    private int seenCount = 1;
}