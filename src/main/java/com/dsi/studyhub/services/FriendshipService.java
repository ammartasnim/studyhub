package com.dsi.studyhub.services;

import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.FriendshipRepository;
import com.dsi.studyhub.entities.Friendship;
import com.dsi.studyhub.entities.FriendshipId;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.FriendshipStatus;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.dtos.FriendshipResDto;
import com.dsi.studyhub.dtos.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Block;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    @Autowired
    private UserRepository UserRepository;

    public FriendshipResDto sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId))
            throw new IllegalArgumentException("You cannot send a friend request to yourself.");

        friendshipRepository.findBetweenUsers(requesterId, addresseeId).ifPresent(f -> {
            throw new IllegalStateException("Friendship already exists with status: " + f.getStatus());
        });



        User requester = userRepository.findById(requesterId).orElseThrow();
        User addressee = userRepository.findById(addresseeId).orElseThrow();

        FriendshipId id = new FriendshipId();
        id.setRequesterId(requesterId);
        id.setAddresseeId(addresseeId);

        Friendship friendship = new Friendship();
        friendship.setId(id);
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);

        return toResDto(friendshipRepository.save(friendship));
    }

    public FriendshipResDto acceptRequest(Long addresseeId, Long requesterId) {
        FriendshipId id = new FriendshipId();
        id.setRequesterId(requesterId);
        id.setAddresseeId(addresseeId);

        Friendship friendship = friendshipRepository.findById(id).orElseThrow();

        if (friendship.getStatus() != FriendshipStatus.PENDING)
            throw new IllegalStateException("Request is not pending.");

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return toResDto(friendshipRepository.save(friendship));
    }

    public void deleteFriendship(Long userId, Long friendId) {
        Friendship friendship = friendshipRepository
                .findBetweenUsers(userId, friendId)
                .orElseThrow();

        friendshipRepository.delete(friendship);
    }

    public Page<UserSummaryDto> getFriends(Long userId, Pageable pageable) {
        return friendshipRepository.findAcceptedFriends(userId, FriendshipStatus.ACCEPTED, pageable)
                .map(f -> f.getRequester().getId().equals(userId) ? f.getAddressee() : f.getRequester())
                .map(this::toUserSummaryDto);
    }

    public Page<FriendshipResDto> getPendingRequests(long userId, Pageable pageable) {
        User u= userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + userId)
        );
        return friendshipRepository.findByAddresseeAndStatus(u, FriendshipStatus.PENDING, pageable)
                .map(this::toResDto);

    }
    public Page<FriendshipResDto> getSentRequests(long userId, Pageable pageable) {
        User u= userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + userId)
        );
        return friendshipRepository.findByRequesterAndStatus(u, FriendshipStatus.PENDING, pageable)
                .map(this::toResDto);
    }
    public Page<UserSummaryDto> getBlockedUsers(Long userId, Pageable pageable) {
        userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        return friendshipRepository.findBlockedByRequester(userId, pageable)
                .map(Friendship::getAddressee)
                .map(this::toUserSummaryDto);
    }
    public boolean isFriend(Long userId, Long friendId) {
        return friendshipRepository.existsAcceptedFriendship(userId, friendId);
    }
    public boolean hasPendingRequest(Long requesterId, Long addresseeId) {
        return friendshipRepository.findBetweenUsers(requesterId, addresseeId)
                .map(f -> f.getRequester().getId().equals(requesterId)
                        && f.getAddressee().getId().equals(addresseeId)
                        && f.getStatus() == FriendshipStatus.PENDING)
                .orElse(false);
    }
    public Page<UserSummaryDto> getSuggestedFriends(Long userId, Pageable pageable, String keyword) {
        userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found"));
        String safeKeyword = keyword == null ? "" : keyword;
        return userRepository.findSuggestedFriends(userId, safeKeyword, pageable)
                .map(this::toUserSummaryDto);
    }

    private FriendshipResDto toResDto(Friendship friendship) {
        User requester = friendship.getRequester();
        User addressee = friendship.getAddressee();
        return new FriendshipResDto(
                friendship.getId() != null ? friendship.getId().getRequesterId() : null,
                friendship.getId() != null ? friendship.getId().getAddresseeId() : null,
                friendship.getStatus(),
                friendship.getCreatedAt(),
                requester == null ? null : toUserSummaryDto(requester),
                addressee == null ? null : toUserSummaryDto(addressee)
        );
    }

    private UserSummaryDto toUserSummaryDto(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getPfp(),
                user.getFirstName(),
                user.getLastName()
        );
    }
    public UserSummaryDto blockUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId))
            throw new IllegalArgumentException("You cannot block yourself.");

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Friendship friendship = friendshipRepository
                .findBetweenUsers(currentUserId, targetUserId)
                .orElse(null);

        if (friendship != null) {
            if (friendship.getStatus() == FriendshipStatus.BLOCKED)
                throw new IllegalStateException("User is already blocked.");

            friendship.setStatus(FriendshipStatus.BLOCKED);
            friendship.getRequester();
            friendshipRepository.save(friendship);
        } else {
            // no prior friendship — create a new blocked row
            FriendshipId id = new FriendshipId();
            id.setRequesterId(currentUserId);
            id.setAddresseeId(targetUserId);
            User u =userRepository.findById(targetUserId).orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );

            Friendship blocked = new Friendship();
            blocked.setId(id);
            blocked.setRequester(userRepository.findById(currentUserId).orElseThrow());
            blocked.setAddressee(target);
            blocked.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(blocked);
        }

        return toUserSummaryDto(target);
    }
    public UserSummaryDto unblockUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId))
            throw new IllegalArgumentException("You cannot unblock yourself.");

        Friendship friendship = friendshipRepository
                .findBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No relationship found."));

        if (friendship.getStatus() != FriendshipStatus.BLOCKED)
            throw new IllegalStateException("User is not blocked.");

        friendshipRepository.delete(friendship);

        return toUserSummaryDto(userRepository.findById(targetUserId).orElseThrow());
    }



}
