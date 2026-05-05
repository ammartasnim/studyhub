package com.dsi.studyhub.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ConversationResDto(
        Long id,
        Long userOneId,
        Long userTwoId,
        LocalDateTime updatedAt,
        MessageResDto lastMessage
) implements Serializable {
}
