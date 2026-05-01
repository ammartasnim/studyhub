package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    List<Community> findByTitleContainingIgnoreCase(String title);

    List<Community> findByDescriptionContainingIgnoreCase(String description);

    // Fix: correct Spring Data keyword is GreaterThanEqual (not GreaterThanOrEqual)
    List<Community> findByNbrMembersGreaterThanEqual(Integer minMembers);
    @Query("SELECT DISTINCT c FROM Community c LEFT JOIN c.members m " +
            "WHERE c.moderator.id = :userId OR m.id = :userId")
    Page<Community> findAllJoinedOrModerated(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Community c LEFT JOIN c.members m " +
            "WHERE c.moderator.id = :userId")
    Page<Community> findModerated(@Param("userId") Long userId, Pageable pageable);


    // Combined, pageable-aware filter method. Parameters are optional (can be null).
    @Query("SELECT c FROM Community c WHERE " +
            "(:title IS NULL OR :title = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:description IS NULL OR :description = '' OR LOWER(c.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
            "(:minMembers IS NULL OR c.nbrMembers >= :minMembers)")
    Page<Community> findByFilters(@Param("title") String title,
                                  @Param("description") String description,
                                  @Param("minMembers") Integer minMembers,
                                  Pageable pageable);

    @Query("SELECT c FROM Community c ORDER BY SIZE(c.members) DESC")
    List<Community> findTopByMemberCount(Pageable pageable);
}

