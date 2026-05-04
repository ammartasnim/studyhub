package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Post;
import com.dsi.studyhub.enums.PostStatus;
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
    long countByStatus(PostStatus status);

    @Query("SELECT p FROM Post p WHERE p.status = com.dsi.studyhub.enums.PostStatus.Approved " +
            "AND p.user.id != :userId " +
            "AND (p.community IN (SELECT c FROM Community c JOIN c.members m WHERE m.id = :userId) " +
            "OR p.community IN (SELECT c FROM Community c WHERE c.owner.id = :userId))")
    List<Post> findCommunityFeedPosts(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p WHERE p.status = com.dsi.studyhub.enums.PostStatus.Approved " +
            "AND p.user.id != :userId " +
            "AND p.community.category IN :categories " +
            "AND p.community NOT IN (SELECT c FROM Community c JOIN c.members m WHERE m.id = :userId) " +
            "AND p.community NOT IN (SELECT c FROM Community c WHERE c.owner.id = :userId)")
    List<Post> findDiscoveryPostsByCategories(@Param("userId") Long userId, @Param("categories") List<String> categories);

    @Query("SELECT p FROM Post p WHERE p.status = com.dsi.studyhub.enums.PostStatus.Approved " +
            "AND p.user.id != :userId")
    List<Post> findAllApprovedExcludingUser(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p WHERE p.status = :status")
    Page<Post> findByStatus(@Param("status") PostStatus status, Pageable pageable);
}