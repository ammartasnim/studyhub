package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import com.dsi.studyhub.dtos.UserFocusRankDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FocusSessionService {
    FocusSessionResDto startSession(FocusSessionReqDto request);
    FocusSessionResDto pauseSession(Long id, Integer remainingSeconds);
    FocusSessionResDto resumeSession(Long id, Integer remainingSeconds);
    FocusSessionResDto completeSession(Long id, String finalTimer);
    Optional<FocusSessionResDto> getActiveSession();
    Page<FocusSessionResDto> getMySessions(Pageable pageable);
    Page<FocusSessionResDto> getSessionsByUserId(Long userId, Pageable pageable);
    List<FocusSessionResDto> getSessionsByUserIdAsList(Long userId);
    void deleteSession(Long id);
    Map<String, Long> getFocusStats();
    List<UserFocusRankDto> getTopFocusUsers();
}
