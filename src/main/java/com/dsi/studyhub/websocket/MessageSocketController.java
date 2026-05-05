package com.dsi.studyhub.websocket;

import com.dsi.studyhub.dtos.ConversationResDto;
import com.dsi.studyhub.dtos.MessageHistoryReqDto;
import com.dsi.studyhub.dtos.MessageReadDto;
import com.dsi.studyhub.dtos.MessageResDto;
import com.dsi.studyhub.dtos.MessageSendDto;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.MessageService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.security.Principal;
import java.util.List;

@Controller
public class MessageSocketController {
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public MessageSocketController(MessageService messageService,
                                   SimpMessagingTemplate messagingTemplate,
                                   UserRepository userRepository) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @MessageMapping("/messages.send")
    public void sendMessage(@Valid @Payload MessageSendDto request, Principal principal) {
        bindSecurityContext(principal);
        MessageResDto saved = messageService.sendMessage(request);
        if (saved.recipientUsername() != null) {
            messagingTemplate.convertAndSendToUser(
                    saved.recipientUsername(),
                    "/queue/messages",
                    saved
            );
        }

    }

    @MessageMapping("/messages.read")
    public void markRead(@Valid @Payload MessageReadDto request, Principal principal) {
        bindSecurityContext(principal);
        messageService.markConversationRead(request);
    }

    @MessageMapping("/messages.conversations")
    public void listConversations(Principal principal) {
        bindSecurityContext(principal);
        List<ConversationResDto> conversations = messageService.getMyConversations();
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/conversations",
                    conversations
            );
        }
    }

    @MessageMapping("/messages.history")
    public void getHistory(@Valid @Payload MessageHistoryReqDto request, Principal principal) {
        bindSecurityContext(principal);
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        Page<MessageResDto> messages = messageService.getConversationMessages(
                request.conversationId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/messages.history",
                    messages
            );
        }
    }

    private void bindSecurityContext(Principal principal) {
        if (principal instanceof Authentication authentication) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return;
        }
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    }
}
