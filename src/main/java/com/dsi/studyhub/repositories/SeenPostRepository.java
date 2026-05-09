package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.SeenPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface SeenPostRepository extends JpaRepository<SeenPost, Long> {

    Optional<SeenPost> findByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT sp.post.id FROM SeenPost sp WHERE sp.user.id = :userId")
    Set<Long> findSeenPostIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM SeenPost sp WHERE sp.lastSeenAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
