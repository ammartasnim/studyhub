package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.FriendshipResDto;
import com.dsi.studyhub.dtos.UserSummaryDto;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.services.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friendships")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request/{addresseeId}")
    public ResponseEntity<FriendshipResDto> sendRequest(
            @PathVariable Long addresseeId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                friendshipService.sendRequest(currentUser.getId(), addresseeId));
    }

    @PutMapping("/accept/{requesterId}")
    public ResponseEntity<FriendshipResDto> acceptRequest(
            @PathVariable Long requesterId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                friendshipService.acceptRequest(currentUser.getId(), requesterId));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriendship(
            @PathVariable Long friendId,
            @AuthenticationPrincipal User currentUser) {
        friendshipService.deleteFriendship(currentUser.getId(), friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-friends")
    public ResponseEntity<Page<UserSummaryDto>> getFriends(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(friendshipService.getFriends(currentUser.getId(), pageable));
    }
    @GetMapping("/pending")
    public ResponseEntity<Page<FriendshipResDto>> getPendingRequests(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(friendshipService.getPendingRequests(currentUser.getId(), pageable));
    }
    @GetMapping("/is-friend/{friendId}")
    public ResponseEntity<Boolean> isFriend(
            @PathVariable Long friendId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                friendshipService.isFriend(currentUser.getId(), friendId));
    }
    @GetMapping("/suggestions")
    public ResponseEntity<Page<UserSummaryDto>> getSuggestedFriends(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                friendshipService.getSuggestedFriends(currentUser.getId(), pageable));
    }


}
