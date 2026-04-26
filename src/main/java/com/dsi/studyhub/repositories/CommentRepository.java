package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}