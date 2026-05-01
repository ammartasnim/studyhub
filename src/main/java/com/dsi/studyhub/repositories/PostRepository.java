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
    @Query("SELECT p FROM Post p WHERE p.community IN " +
            "(SELECT c FROM Community c JOIN c.members m WHERE m.id = :userId) " +
            "AND p.status = com.dsi.studyhub.enums.PostStatus.Approved " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findFeedForUser(@Param("userId") Long userId, Pageable pageable);

}
