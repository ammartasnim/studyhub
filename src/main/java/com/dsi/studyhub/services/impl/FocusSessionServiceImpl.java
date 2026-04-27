package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import com.dsi.studyhub.entities.FocusSession;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.mappers.FocusSessionMapper;
import com.dsi.studyhub.repositories.FocusSessionRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.FocusSessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FocusSessionServiceImpl implements FocusSessionService {
    @Autowired
    private FocusSessionRepository focusSessionRepository;
    @Autowired
    private FocusSessionMapper focusSessionMapper;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public FocusSessionResDto saveSession(FocusSessionReqDto request) {
        User user = authenticatedUserService.getAuthenticatedUser();

        FocusSession session = new FocusSession();
        session.setTitle(request.title());
        session.setTimer(request.timer());
        session.setUser(user);

        FocusSession saved = focusSessionRepository.save(session);
        return focusSessionMapper.toDto(saved);
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
    public void deleteSession(Long id) {
        if (!focusSessionRepository.existsById(id)) {
            throw new RuntimeException("FocusSession not found");
        }
        focusSessionRepository.deleteById(id);
    }
}
