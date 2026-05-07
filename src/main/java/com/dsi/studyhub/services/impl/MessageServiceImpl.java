package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.ConversationResDto;
import com.dsi.studyhub.dtos.MessageReadDto;
import com.dsi.studyhub.dtos.MessageResDto;
import com.dsi.studyhub.dtos.MessageSendDto;
import com.dsi.studyhub.entities.Conversation;
import com.dsi.studyhub.entities.Message;
import com.dsi.studyhub.enums.MessageStatus;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.ConversationRepository;
import com.dsi.studyhub.repositories.MessageRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.FriendshipService;
import com.dsi.studyhub.services.MessageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;
    @Autowired
    private FriendshipService friendshipService;

    @Override
        @Transactional
        public MessageResDto sendMessage(MessageSendDto request) {
            User sender = authenticatedUserService.getAuthenticatedUser();
            if (sender.getId().equals(request.recipientId())) {
                throw new IllegalArgumentException("You cannot message yourself.");
            }

            User recipient = userRepository.findById(request.recipientId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!friendshipService.isFriend(sender.getId(), recipient.getId())) {
                throw new ForbiddenException("You can only message friends.");
            }

            Conversation conversation = conversationRepository
                    .findBetweenUsers(sender.getId(), recipient.getId())
                    .orElseGet(() -> createConversation(sender.getId(), recipient.getId()));

            Message message = new Message();
            message.setConversation(conversation);
            message.setSender(sender);
            message.setContent(request.content());

            Message saved = messageRepository.save(message);
            conversation.setUpdatedAt(saved.getCreatedAt());
            conversationRepository.save(conversation);

            return toMessageDto(saved, recipient);
        }

    @Override
    @Transactional
    public Page<MessageResDto> getConversationMessages(Long conversationId, Pageable pageable) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        ensureConversationMember(conversation, currentUser.getId());

        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(m -> toMessageDto(m, resolveRecipient(conversation, m.getSender())));
    }

    @Override
    @Transactional
    public List<ConversationResDto> getMyConversations() {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Long userId = currentUser.getId();

        return conversationRepository.findByUserOneIdOrUserTwoId(userId, userId).stream()
                .map(this::toConversationDto)
                .sorted(java.util.Comparator.comparing(ConversationResDto::updatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markConversationRead(MessageReadDto request) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        ensureConversationMember(conversation, currentUser.getId());
        messageRepository.markConversationMessages(
                conversation.getId(),
                currentUser.getId(),
                MessageStatus.READ
        );
    }

    private Conversation createConversation(Long userA, Long userB) {
        Conversation conversation = new Conversation();
        conversation.setUserOneId(Math.min(userA, userB));
        conversation.setUserTwoId(Math.max(userA, userB));
        return conversationRepository.save(conversation);
    }

    private void ensureConversationMember(Conversation conversation, Long userId) {
        if (!conversation.getUserOneId().equals(userId) && !conversation.getUserTwoId().equals(userId)) {
            throw new ForbiddenException("You are not part of this conversation.");
        }
    }

    private User resolveRecipient(Conversation conversation, User sender) {
        if (sender == null) {
            return null;
        }
        Long recipientId = conversation.getUserOneId().equals(sender.getId())
                ? conversation.getUserTwoId()
                : conversation.getUserOneId();
        return userRepository.findById(recipientId).orElse(null);
    }

    private MessageResDto toMessageDto(Message message, User recipient) {
        String senderUsername = message.getSender() != null ? message.getSender().getUsername() : null;
        Long recipientId = recipient != null ? recipient.getId() : null;
        String recipientUsername = recipient != null ? recipient.getUsername() : null;
        return new MessageResDto(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                senderUsername,
                recipientId,
                recipientUsername,
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }

    private ConversationResDto toConversationDto(Conversation conversation) {
        Optional<Message> lastMessage = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                        conversation.getId(),
                        Pageable.ofSize(1))
                .stream()
                .findFirst();

        MessageResDto lastDto = lastMessage
                .map(m -> toMessageDto(m, resolveRecipient(conversation, m.getSender())))
                .orElse(null);

        return new ConversationResDto(
                conversation.getId(),
                conversation.getUserOneId(),
                conversation.getUserTwoId(),
                conversation.getUpdatedAt(),
                lastDto
        );
    }
}
