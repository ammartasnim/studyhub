package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    @Query("""
            SELECT c
            FROM Community c
            WHERE (:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (:description IS NULL OR LOWER(c.description) LIKE LOWER(CONCAT('%', :description, '%')))
              AND (:minMembers IS NULL OR c.nbrMembers >= :minMembers)
            """)
    Page<Community> findAllWithFilters(
            @Param("title") String title,
            @Param("description") String description,
            @Param("minMembers") Integer minMembers,
            Pageable pageable
    );
}

