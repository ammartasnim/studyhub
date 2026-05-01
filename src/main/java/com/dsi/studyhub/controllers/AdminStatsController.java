package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.CommunityResDto;
import com.dsi.studyhub.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStatsController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommunityRepository communityRepository;
    private final FocusSessionRepository focusSessionRepository;

//    @GetMapping("/overview")
//    public ResponseEntity<AdminOverviewDto> getOverview() {
//        AdminOverviewDto stats = new AdminOverviewDto(
//                userRepository.count(),
//                userRepository.countByBanned(true),
//                postRepository.count(),
//                postRepository.countByStatus("FLAGGED"),
//                postRepository.countByStatus("PENDING"),
//                commentRepository.count(),
//                communityRepository.count(),
//                focusSessionRepository.countByStatus(SessionStatus.COMPLETED),
//                focusSessionRepository.countByStatus(SessionStatus.ACTIVE)
//        );
//        return ResponseEntity.ok(stats);
//    }
//
//    @GetMapping("/users/growth")
//    public ResponseEntity<List<DailyCountDto>> getUserGrowth(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
//        return ResponseEntity.ok(userRepository.countByCreatedAtBetweenGroupByDay(from, to));
//    }
//
//    @GetMapping("/posts/growth")
//    public ResponseEntity<List<DailyCountDto>> getPostGrowth(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
//        return ResponseEntity.ok(postRepository.countByCreatedAtBetweenGroupByDay(from, to));
//    }
//
//    @GetMapping("/focus-sessions/stats")
//    public ResponseEntity<FocusStatsDto> getFocusStats() {
//        FocusStatsDto stats = new FocusStatsDto(
//                focusSessionRepository.countByStatus(SessionStatus.COMPLETED),
//                focusSessionRepository.averageCompletedSessionMinutes(),
//                focusSessionRepository.findTopUsersByFocusTime(PageRequest.of(0, 5))
//        );
//        return ResponseEntity.ok(stats);
//    }
//
//    @GetMapping("/communities/top")
//    public ResponseEntity<List<CommunityResDto>> getTopCommunities() {
//        return ResponseEntity.ok(communityRepository.findTopByMemberCount(PageRequest.of(0, 5)));
//    }
//
//    @GetMapping("/badges/distribution")
//    public ResponseEntity<Map<String, Long>> getBadgeDistribution() {
//        return ResponseEntity.ok(userRepository.countByBadgeGrouped());
//    }
}