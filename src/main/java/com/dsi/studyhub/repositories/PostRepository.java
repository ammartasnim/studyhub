package com.dsi.studyhub.repositories;

import com.dsi.studyhub.dtos.PostResDto;
import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findById(long id);
    Page<Post> findAll(Pageable pageable);
    Page<Post> findByCommunityId(Long communityId, Pageable pageable);
    Page<Post> findByUserId(Long userId, Pageable pageable);
    Page<Post> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    // Posts from communities user joined or moderated, excluding user's own posts
    @Query("SELECT p FROM Post p WHERE p.status = com.dsi.studyhub.enums.PostStatus.Approved " +
            "AND p.user.id != :userId " +
            "AND p.community IN (" +
            "  SELECT c FROM Community c JOIN c.members m WHERE m.id = :userId " +
            "  UNION " +
            "  SELECT c FROM Community c WHERE c.moderator.id = :userId" +
            ")")
    List<Post> findCommunityFeedPosts(@Param("userId") Long userId);

    // Discovery posts from communities user is NOT in, matching categories
    @Query("SELECT p FROM Post p WHERE p.status = com.dsi.studyhub.enums.PostStatus.Approved " +
            "AND p.user.id != :userId " +
            "AND p.community NOT IN (" +
            "  SELECT c FROM Community c JOIN c.members m WHERE m.id = :userId " +
            "  UNION " +
            "  SELECT c FROM Community c WHERE c.moderator.id = :userId" +
            ") " +
            "AND p.community.category IN :categories")
    List<Post> findDiscoveryPostsByCategories(@Param("userId") Long userId, @Param("categories") List<String> categories);

    // Fallback discovery when user has no communities
    @Query("SELECT p FROM Post p WHERE p.status = com.dsi.studyhub.enums.PostStatus.Approved " +
            "AND p.user.id != :userId")
    List<Post> findAllApprovedExcludingUser(@Param("userId") Long userId);

}
