package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import com.dsi.studyhub.services.FocusSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/focus-sessions")
public class FocusSessionController {
    @Autowired
    private FocusSessionService focusSessionService;

    @PostMapping
    public ResponseEntity<FocusSessionResDto> createSession(@RequestBody FocusSessionReqDto request) {
        return new ResponseEntity<>(focusSessionService.saveSession(request), HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FocusSessionResDto>> getUserSessions(@PathVariable Long userId) {
        return ResponseEntity.ok(focusSessionService.getSessionsByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        focusSessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
