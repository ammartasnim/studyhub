package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FocusSessionServiceImpl implements FocusSessionService {
    @Autowired
    private FocusSessionRepository focusSessionRepository;
    @Autowired
    private FocusSessionMapper focusSessionMapper;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

//    @Override
//    public FocusSessionResDto saveSession(FocusSessionReqDto request) {
//        User user = authenticatedUserService.getAuthenticatedUser();
//
//        FocusSession session = new FocusSession();
//        session.setTitle(request.title());
//        session.setTimer(request.timer());
//        session.setUser(user);
//
//        FocusSession saved = focusSessionRepository.save(session);
//        return focusSessionMapper.toDto(saved);
//    }
@Override
@Transactional
public FocusSessionResDto startSession(FocusSessionReqDto request) {
    User user = authenticatedUserService.getAuthenticatedUser();

    boolean hasActive = focusSessionRepository
            .findFirstByUserIdAndStatusInOrderByLastUpdatedDesc(
                    user.getId(), List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED))
            .isPresent();
    if (hasActive) {
        throw new IllegalStateException("You already have an active or paused session.");
    }

    FocusSession session = new FocusSession();
    session.setTitle(request.title());
    // We set the initial countdown (e.g., 1500 for 25 mins)
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
        session.setLastUpdated(LocalDateTime.now()); // resets the clock baseline

        return focusSessionMapper.toDto(focusSessionRepository.save(session));
    }

    @Override
    @Transactional
    public Optional<FocusSessionResDto> getActiveSession() {
        User user = authenticatedUserService.getAuthenticatedUser();

        return focusSessionRepository.findFirstByUserIdAndStatusNotOrderByLastUpdatedDesc(
                        user.getId(), SessionStatus.COMPLETED)
                .flatMap(session -> {
                    // 2. If it was RUNNING when we left, calculate the "lost" time
                    if (session.getStatus() == SessionStatus.ACTIVE) {
                        long secondsPassed = java.time.Duration.between(
                                session.getLastUpdated(), LocalDateTime.now()
                        ).getSeconds();

                        int newRemaining = (int) (session.getRemainingSeconds() - secondsPassed);
                        if (newRemaining <= 0) {
                            session.setStatus(SessionStatus.COMPLETED);
                            session.setRemainingSeconds(0);
                            session.setLastUpdated(LocalDateTime.now());
                            focusSessionRepository.save(session); // persist the completion
                            return Optional.<FocusSessionResDto>empty();// or return it as completed — your choice
                        }
                        session.setRemainingSeconds(Math.max(0, newRemaining));
                        session.setLastUpdated(LocalDateTime.now());
                        focusSessionRepository.save(session);
                    }
                    // 3. If PAUSED, we just return it as is (time didn't move)
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

    @Override
    @Transactional
    public void deleteSession(Long id) {
        if (!focusSessionRepository.existsById(id)) {
            throw new RuntimeException("FocusSession not found");
        }
        focusSessionRepository.deleteById(id);
    }
}
