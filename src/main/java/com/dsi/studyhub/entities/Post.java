package com.dsi.studyhub.entities;

import com.dsi.studyhub.enums.PostStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(indexes = {
        @Index(name = "idx_post_user_id", columnList = "user_id"),
        @Index(name = "idx_post_community_id", columnList = "community_id"),
        @Index(name = "idx_post_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "post_imgs", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "img_url")
    private List<String> imgs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status=PostStatus.Pending;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "community_id")
    private Community community;


    @ManyToMany(mappedBy = "likedPosts")
    private List<User> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

}
