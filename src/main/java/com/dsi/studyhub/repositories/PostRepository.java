package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findById(long id);
    List<Post> findAll();
}
