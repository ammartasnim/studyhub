package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.ConversationResDto;
import com.dsi.studyhub.dtos.MessageReadDto;
import com.dsi.studyhub.dtos.MessageResDto;
import com.dsi.studyhub.dtos.MessageSendDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageService {
    MessageResDto sendMessage(MessageSendDto request);
    Page<MessageResDto> getConversationMessages(Long conversationId, Pageable pageable);
    List<ConversationResDto> getMyConversations();
    void markConversationRead(MessageReadDto request);
}
