package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.FriendshipResDto;
import com.dsi.studyhub.dtos.UserSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FriendshipService {
    FriendshipResDto sendRequest(Long requesterId, Long addresseeId);
    FriendshipResDto acceptRequest(Long addresseeId, Long requesterId);
    void deleteFriendship(Long userId, Long friendId);
    Page<UserSummaryDto> getFriends(Long userId, Pageable pageable);
    Page<FriendshipResDto> getPendingRequests(long userId, Pageable pageable);
    Page<FriendshipResDto> getSentRequests(long userId, Pageable pageable);
    Page<UserSummaryDto> getBlockedUsers(Long userId, Pageable pageable);
    boolean isFriend(Long userId, Long friendId);
    boolean hasPendingRequest(Long requesterId, Long addresseeId);
    Page<UserSummaryDto> getSuggestedFriends(Long userId, Pageable pageable, String keyword);
    UserSummaryDto blockUser(Long currentUserId, Long targetUserId);
    UserSummaryDto unblockUser(Long currentUserId, Long targetUserId);
    List<UserSummaryDto> searchFriends(Long userId, String query);
}
