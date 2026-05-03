package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByUserId(Long userId, Pageable pageable);
    Page<Comment> findByPostId(Long postId, Pageable pageable);
    Page<Comment> findByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);
    Page<Comment> findByParentCommentId(Long parentCommentId, Pageable pageable);
}