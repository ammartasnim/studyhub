package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import com.dsi.studyhub.dtos.UserFocusRankDto;
import com.dsi.studyhub.entities.FocusSession;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.SessionStatus;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.mappers.FocusSessionMapper;
import com.dsi.studyhub.repositories.FocusSessionRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.FocusSessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FocusSessionServiceImpl implements FocusSessionService {
    @Autowired
    private FocusSessionRepository focusSessionRepository;
    @Autowired
    private FocusSessionMapper focusSessionMapper;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    // Session lifecycle
    @Override
    @Transactional
    public FocusSessionResDto startSession(FocusSessionReqDto request) {
    User user = authenticatedUserService.getAuthenticatedUser();

    boolean hasActive = focusSessionRepository
            .findFirstByUserIdAndStatusInOrderByLastUpdatedDesc(
                    user.getId(), List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED))
            .isPresent();
    if (hasActive) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT, "You already have a session in progress."
        );
    }

    FocusSession session = new FocusSession();
    session.setTitle(request.title());
    session.setRemainingSeconds(request.remainingSeconds());
    session.setLastUpdated(LocalDateTime.now());
    session.setStatus(SessionStatus.ACTIVE);
    session.setUser(user);

    return focusSessionMapper.toDto(focusSessionRepository.save(session));
}

    @Override
    @Transactional
    public FocusSessionResDto pauseSession(Long id, Integer remainingSeconds) {
        FocusSession session = focusSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setStatus(SessionStatus.PAUSED);
        session.setRemainingSeconds(remainingSeconds);
        session.setLastUpdated(LocalDateTime.now());

        return focusSessionMapper.toDto(focusSessionRepository.save(session));
    }

    @Override
    @Transactional
    public FocusSessionResDto resumeSession(Long id, Integer remainingSeconds) {
        FocusSession session = focusSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setStatus(SessionStatus.ACTIVE);
        session.setRemainingSeconds(remainingSeconds);
        session.setLastUpdated(LocalDateTime.now());

        return focusSessionMapper.toDto(focusSessionRepository.save(session));
    }

    @Override
    @Transactional
    public Optional<FocusSessionResDto> getActiveSession() {
        // Recomputes remaining time when resuming an active session.
        User user = authenticatedUserService.getAuthenticatedUser();

        return focusSessionRepository.findFirstByUserIdAndStatusInOrderByLastUpdatedDesc(
                        user.getId(), List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED))
                .flatMap(session -> {
                    if (session.getStatus() == SessionStatus.ACTIVE) {
                        long secondsPassed = java.time.Duration.between(
                                session.getLastUpdated(), LocalDateTime.now()
                        ).getSeconds();

                        int newRemaining = (int) (session.getRemainingSeconds() - secondsPassed);
                        if (newRemaining <= 0) {
                            session.setStatus(SessionStatus.COMPLETED);
                            session.setRemainingSeconds(0);
                            session.setLastUpdated(LocalDateTime.now());
                            focusSessionRepository.save(session);
                            return Optional.<FocusSessionResDto>empty();
                        }
                        session.setRemainingSeconds(Math.max(0, newRemaining));
                        session.setLastUpdated(LocalDateTime.now());
                        focusSessionRepository.save(session);
                    }
                    return Optional.of(focusSessionMapper.toDto(session));
                });
    }

    @Override
    @Transactional
    public FocusSessionResDto completeSession(Long id, String finalTimer) {
        FocusSession session = focusSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setStatus(SessionStatus.COMPLETED);
        session.setTimer(finalTimer);
        session.setRemainingSeconds(0);
        session.setLastUpdated(LocalDateTime.now());

        return focusSessionMapper.toDto(focusSessionRepository.save(session));
    }

    // Session queries
    @Override
    public Page<FocusSessionResDto> getMySessions(Pageable pageable) {
        User user = authenticatedUserService.getAuthenticatedUser();
        return focusSessionRepository.findByUserId(user.getId(), pageable)
                .map(focusSessionMapper::toDto);
    }

    @Override
    @Transactional
    public Page<FocusSessionResDto> getSessionsByUserId(Long userId, Pageable pageable) {
        return focusSessionRepository.findByUserId(userId, pageable)
                .map(focusSessionMapper::toDto);
    }

    @Override
    @Transactional
    public List<FocusSessionResDto> getSessionsByUserIdAsList(Long userId) {
        return focusSessionRepository.findByUserId(userId)
                .stream()
                .map(focusSessionMapper::toDto)
                .toList();
    }

    // Session deletion
    @Override
    @Transactional
    public void deleteSession(Long id) {
        if (!focusSessionRepository.existsById(id)) {
            throw new RuntimeException("FocusSession not found");
        }
        focusSessionRepository.deleteById(id);
    }

    // Focus stats and growth
    @Override
    public Map<String, Long> getFocusStats() {
        return Map.of(
                "completed", focusSessionRepository.countByStatus(SessionStatus.COMPLETED),
                "active", focusSessionRepository.countByStatus(SessionStatus.ACTIVE)
        );
    }

    @Override
    public List<UserFocusRankDto> getTopFocusUsers() {
        return focusSessionRepository.findTopUsersByFocusTime(PageRequest.of(0, 5));
    }


    @Override
    public List<Map<String, Object>> getUserGrowth() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = focusSessionRepository.countByStatusAndLastUpdatedBetween(SessionStatus.COMPLETED,
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay()
            );
            result.add(Map.of("date", date.toString(), "count", count));
        }
        return result;
    }
}
