package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.FriendshipStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public record FriendshipResDto(
        Long requesterId,
        Long addresseeId,
        FriendshipStatus status,
        LocalDateTime createdAt,
        UserSummaryDto requester,
        UserSummaryDto addressee
) implements Serializable {
}
