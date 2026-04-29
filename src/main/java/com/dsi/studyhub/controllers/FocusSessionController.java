package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.FocusSessionReqDto;
import com.dsi.studyhub.dtos.FocusSessionResDto;
import com.dsi.studyhub.services.FocusSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/focus-sessions")
public class FocusSessionController {
    @Autowired
    private FocusSessionService focusSessionService;

//    @PostMapping
//    public ResponseEntity<FocusSessionResDto> createSession(@RequestBody FocusSessionReqDto request) {
//        return new ResponseEntity<>(focusSessionService.saveSession(request), HttpStatus.CREATED);
//    }
    @PostMapping("/start")
    public ResponseEntity<FocusSessionResDto> start(@RequestBody FocusSessionReqDto request) {
        return ResponseEntity.ok(focusSessionService.startSession(request));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<FocusSessionResDto> pause(
            @PathVariable Long id,
            @RequestParam Integer remainingSeconds) {
        return ResponseEntity.ok(focusSessionService.pauseSession(id, remainingSeconds));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<FocusSessionResDto> resume(
            @PathVariable Long id,
            @RequestParam Integer remainingSeconds) {
        return ResponseEntity.ok(focusSessionService.resumeSession(id, remainingSeconds));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<FocusSessionResDto> complete(
            @PathVariable Long id,
            @RequestParam String finalTimer) {
        return ResponseEntity.ok(focusSessionService.completeSession(id, finalTimer));
    }

    @GetMapping("/active")
    public ResponseEntity<FocusSessionResDto> getActive() {
        return focusSessionService.getActiveSession()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FocusSessionResDto>> getUserSessions(@PathVariable Long userId) {
        return ResponseEntity.ok(focusSessionService.getSessionsByUserIdAsList(userId));
    }

    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<Page<FocusSessionResDto>> getUserSessionsPaginated(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(focusSessionService.getSessionsByUserId(userId, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<FocusSessionResDto>> getMySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(focusSessionService.getMySessions(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        focusSessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
