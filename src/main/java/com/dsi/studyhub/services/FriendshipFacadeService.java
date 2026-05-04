package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.FriendshipResDto;
import com.dsi.studyhub.dtos.UserSummaryDto;
import com.dsi.studyhub.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendshipFacadeService {

    private final FriendshipService friendshipService;
    private final AuthenticatedUserService authenticatedUserService;

    public FriendshipResDto sendRequest(Long addresseeId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return friendshipService.sendRequest(currentUser.getId(), addresseeId);
    }

    public FriendshipResDto acceptRequest(Long requesterId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return friendshipService.acceptRequest(currentUser.getId(), requesterId);
    }

    public void deleteFriendship(Long friendId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        friendshipService.deleteFriendship(currentUser.getId(), friendId);
    }

    public Page<UserSummaryDto> getFriends(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return friendshipService.getFriends(currentUser.getId(), pageable);
    }

    public Page<FriendshipResDto> getPendingRequests(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return friendshipService.getPendingRequests(currentUser.getId(), pageable);
    }

    public boolean isFriend(Long friendId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return friendshipService.isFriend(currentUser.getId(), friendId);
    }

    public Page<UserSummaryDto> getSuggestedFriends(Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return friendshipService.getSuggestedFriends(currentUser.getId(), pageable);
    }
}
