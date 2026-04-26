package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import com.dsi.studyhub.entities.FocusSession;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.mappers.FocusSessionMapper;
import com.dsi.studyhub.repositories.FocusSessionRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.FocusSessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FocusSessionServiceImpl implements FocusSessionService {
    @Autowired
    private FocusSessionRepository focusSessionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FocusSessionMapper focusSessionMapper;

    @Override
    public FocusSessionResDto saveSession(FocusSessionReqDto request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        FocusSession session = new FocusSession();
        session.setTitle(request.title());
        session.setTimer(request.timer());
        session.setUser(user);

        FocusSession saved = focusSessionRepository.save(session);
        return focusSessionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public List<FocusSessionResDto> getSessionsByUserId(Long userId) {
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
