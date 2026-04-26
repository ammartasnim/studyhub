package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findById(long id);
    @Query("select p from Post p where " +
            "(:title is null or lower(p.title) like lower(concat('%', :title, '%')))")
    List<Post> findByTitle(@Param("title") String title);
    List<Post> findAll(  );
}
