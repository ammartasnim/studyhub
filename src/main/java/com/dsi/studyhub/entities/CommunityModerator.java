package com.dsi.studyhub.entities;

import com.dsi.studyhub.enums.CommunityPermission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "community_moderators",
        indexes = {
                @Index(name = "idx_community_mod_user", columnList = "user_id"),
                @Index(name = "idx_community_mod_community", columnList = "community_id"),
                @Index(name = "idx_community_mod_unique", columnList = "user_id, community_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CommunityModerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "community_moderator_permissions",
            joinColumns = @JoinColumn(name = "community_moderator_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<CommunityPermission> permissions = new HashSet<>();

    public CommunityModerator(User user, Community community, Set<CommunityPermission> permissions) {
        this.user = user;
        this.community = community;
        this.permissions = permissions;
    }
}