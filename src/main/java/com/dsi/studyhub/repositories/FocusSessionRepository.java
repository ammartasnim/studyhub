package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.FocusSession;
import com.dsi.studyhub.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findByUserId(Long userId);
    Page<FocusSession> findByUserId(Long userId, Pageable pageable);
    Optional<FocusSession> findFirstByUserIdAndStatusNotOrderByLastUpdatedDesc(Long userId, SessionStatus status);
    Optional<FocusSession> findFirstByUserIdAndStatusInOrderByLastUpdatedDesc(Long userId, List<SessionStatus> status);

}
