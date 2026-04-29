package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FocusSessionService {
    FocusSessionResDto saveSession(FocusSessionReqDto request);
    Page<FocusSessionResDto> getMySessions(Pageable pageable);
    Page<FocusSessionResDto> getSessionsByUserId(Long userId, Pageable pageable);
    List<FocusSessionResDto> getSessionsByUserIdAsList(Long userId);
    void deleteSession(Long id);
}
