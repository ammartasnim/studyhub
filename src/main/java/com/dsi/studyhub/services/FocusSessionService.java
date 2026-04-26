package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;

import java.util.List;

public interface FocusSessionService {
    FocusSessionResDto saveSession(FocusSessionReqDto request);
    List<FocusSessionResDto> getSessionsByUserId(Long userId);
    void deleteSession(Long id);
}
