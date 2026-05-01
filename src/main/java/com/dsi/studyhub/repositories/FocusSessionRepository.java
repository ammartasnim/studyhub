package com.dsi.studyhub.repositories;

import com.dsi.studyhub.dtos.UserFocusRankDto;
import com.dsi.studyhub.entities.FocusSession;
import com.dsi.studyhub.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findByUserId(Long userId);
    Page<FocusSession> findByUserId(Long userId, Pageable pageable);
    Optional<FocusSession> findFirstByUserIdAndStatusNotOrderByLastUpdatedDesc(Long userId, SessionStatus status);
    Optional<FocusSession> findFirstByUserIdAndStatusInOrderByLastUpdatedDesc(Long userId, List<SessionStatus> status);
    long countByStatus(SessionStatus status);

    @Query("""
    SELECT new com.dsi.studyhub.dtos.UserFocusRankDto(
        f.user.id,
        f.user.username,
        SUM(CAST(SUBSTRING(f.timer, 1, 2) AS int) * 60 + CAST(SUBSTRING(f.timer, 4, 2) AS int)) / 60.0
    )
    FROM FocusSession f
    WHERE f.status = 'COMPLETED'
    GROUP BY f.user.id, f.user.username
    ORDER BY SUM(CAST(SUBSTRING(f.timer, 1, 2) AS int) * 60 + CAST(SUBSTRING(f.timer, 4, 2) AS int)) DESC
    """)
    List<UserFocusRankDto> findTopUsersByFocusTime(Pageable pageable);
}
