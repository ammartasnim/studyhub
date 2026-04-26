package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import com.dsi.studyhub.entities.FocusSession;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.repositories.FocusSessionRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.FocusSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FocusSessionServiceImpl implements FocusSessionService {
    @Autowired
    private FocusSessionRepository focusSessionRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public FocusSessionResDto saveSession(FocusSessionReqDto request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        FocusSession session = new FocusSession();
        session.setTitle(request.title());
        session.setTimer(request.timer());
        session.setUser(user);

        FocusSession saved = focusSessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Override
    public List<FocusSessionResDto> getSessionsByUserId(Long userId) {
        return List.of();
    }

    @Override
    public void deleteSession(Long id) {

    }
}
