package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Comment;
import com.dsi.studyhub.enums.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId AND c.status != com.dsi.studyhub.enums.CommentStatus.Flagged")
    Page<Comment> findByUserId(@Param("userId") Long userId, Pageable pageable);
    Page<Comment> findByPostId(Long postId, Pageable pageable);
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.status != com.dsi.studyhub.enums.CommentStatus.Flagged")
    Page<Comment> findByPostIdAndParentCommentIsNull(@Param("postId") Long postId, Pageable pageable);
    @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :commentId AND c.status != com.dsi.studyhub.enums.CommentStatus.Flagged")
    Page<Comment> findByParentCommentId(@Param("commentId") Long commentId, Pageable pageable);

    Long countByStatusNot(CommentStatus commentStatus);
}