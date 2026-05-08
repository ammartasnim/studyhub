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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FocusSessionServiceImplTest {

    @Mock private FocusSessionRepository focusSessionRepository;
    @Mock private FocusSessionMapper focusSessionMapper;
    @Mock private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private FocusSessionServiceImpl focusSessionService;

    private User testUser;
    private FocusSession activeSession;
    private FocusSessionResDto sessionDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");

        activeSession = new FocusSession();
        activeSession.setId(10L);
        activeSession.setTitle("Math Study");
        activeSession.setRemainingSeconds(1500);
        activeSession.setStatus(SessionStatus.ACTIVE);
        activeSession.setLastUpdated(LocalDateTime.now());
        activeSession.setUser(testUser);

        // FocusSessionResDto(Long id, String title, String timer, Integer remainingSeconds,
        //                    String status, LocalDateTime lastUpdated, Long userId)
        sessionDto = new FocusSessionResDto(
                10L, "Math Study", null, 1500,
                "ACTIVE", LocalDateTime.now(), 1L
        );
    }

    // ── START SESSION ────────────────────────────────────────────────────────

    @Test
    void startSession_noExistingSession_createsAndReturnsSession() {
        // FocusSessionReqDto(String title, String timer, Integer remainingSeconds, Long userId)
        FocusSessionReqDto request = new FocusSessionReqDto("Math Study", null, 1500, null);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(focusSessionRepository.findFirstByUserIdAndStatusInOrderByLastUpdatedDesc(
                testUser.getId(), List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED)))
                .thenReturn(Optional.empty());
        when(focusSessionRepository.save(any(FocusSession.class))).thenReturn(activeSession);
        when(focusSessionMapper.toDto(activeSession)).thenReturn(sessionDto);

        FocusSessionResDto result = focusSessionService.startSession(request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Math Study");
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(focusSessionRepository).save(any(FocusSession.class));
    }

    @Test
    void startSession_sessionAlreadyActive_throwsConflictException() {
        FocusSessionReqDto request = new FocusSessionReqDto("Physics Study", null, 1500, null);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(testUser);
        when(focusSessionRepository.findFirstByUserIdAndStatusInOrderByLastUpdatedDesc(
                testUser.getId(), List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED)))
                .thenReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> focusSessionService.startSession(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("session in progress");

        verify(focusSessionRepository, never()).save(any());
    }

    // ── PAUSE SESSION ────────────────────────────────────────────────────────

    @Test
    void pauseSession_activeSession_setsStatusToPaused() {
        when(focusSessionRepository.findById(10L)).thenReturn(Optional.of(activeSession));
        when(focusSessionRepository.save(any(FocusSession.class))).thenReturn(activeSession);
        when(focusSessionMapper.toDto(activeSession)).thenReturn(sessionDto);

        focusSessionService.pauseSession(10L, 900);

        assertThat(activeSession.getStatus()).isEqualTo(SessionStatus.PAUSED);
        assertThat(activeSession.getRemainingSeconds()).isEqualTo(900);
        verify(focusSessionRepository).save(activeSession);
    }

    @Test
    void pauseSession_sessionNotFound_throwsException() {
        when(focusSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> focusSessionService.pauseSession(99L, 900))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");
    }

    // ── RESUME SESSION ───────────────────────────────────────────────────────

    @Test
    void resumeSession_pausedSession_setsStatusBackToActive() {
        activeSession.setStatus(SessionStatus.PAUSED);
        activeSession.setRemainingSeconds(900);

        when(focusSessionRepository.findById(10L)).thenReturn(Optional.of(activeSession));
        when(focusSessionRepository.save(any(FocusSession.class))).thenReturn(activeSession);
        when(focusSessionMapper.toDto(activeSession)).thenReturn(sessionDto);

        focusSessionService.resumeSession(10L, 900);

        assertThat(activeSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        verify(focusSessionRepository).save(activeSession);
    }

    // ── COMPLETE SESSION ─────────────────────────────────────────────────────

    @Test
    void completeSession_activeSession_setsStatusToCompleted() {
        when(focusSessionRepository.findById(10L)).thenReturn(Optional.of(activeSession));
        when(focusSessionRepository.save(any(FocusSession.class))).thenReturn(activeSession);
        when(focusSessionMapper.toDto(activeSession)).thenReturn(sessionDto);

        focusSessionService.completeSession(10L, "25:00");

        assertThat(activeSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(activeSession.getRemainingSeconds()).isEqualTo(0);
        assertThat(activeSession.getTimer()).isEqualTo("25:00");
    }

    @Test
    void completeSession_sessionNotFound_throwsException() {
        when(focusSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> focusSessionService.completeSession(99L, "25:00"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── DELETE SESSION ───────────────────────────────────────────────────────

    @Test
    void deleteSession_existingSession_deletesSuccessfully() {
        when(focusSessionRepository.existsById(10L)).thenReturn(true);

        focusSessionService.deleteSession(10L);

        verify(focusSessionRepository).deleteById(10L);
    }

    @Test
    void deleteSession_sessionNotFound_throwsException() {
        when(focusSessionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> focusSessionService.deleteSession(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("FocusSession not found");
    }
}