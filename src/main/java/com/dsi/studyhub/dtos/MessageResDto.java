package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.MessageStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public record MessageResDto(
        Long id,
        Long conversationId,
        Long senderId,
        String senderUsername,
        Long recipientId,
        String recipientUsername,
        String content,
        MessageStatus status,
        LocalDateTime createdAt
) implements Serializable {
}
