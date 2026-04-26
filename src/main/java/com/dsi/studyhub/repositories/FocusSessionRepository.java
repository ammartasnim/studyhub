package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findByUserId(Long userId);
}
