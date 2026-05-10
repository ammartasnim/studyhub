package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.ConversationResDto;
import com.dsi.studyhub.dtos.MessageReadDto;
import com.dsi.studyhub.dtos.MessageResDto;
import com.dsi.studyhub.dtos.MessageSendDto;
import com.dsi.studyhub.entities.Conversation;
import com.dsi.studyhub.entities.Message;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.MessageStatus;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.ConversationRepository;
import com.dsi.studyhub.repositories.MessageRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.FriendshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private FriendshipService friendshipService;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User sender;
    private User recipient;
    private Conversation conversation;
    private Message message;

    // Builds shared fixtures to keep each test focused on one behavior.
    @BeforeEach
    void setUp() {
        sender = buildUser(1L, "sender");
        recipient = buildUser(2L, "recipient");

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserOneId(sender.getId());
        conversation.setUserTwoId(recipient.getId());
        conversation.setUpdatedAt(LocalDateTime.now());

        message = new Message();
        message.setId(100L);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent("hello");
        message.setStatus(MessageStatus.SENT);
        message.setCreatedAt(LocalDateTime.now());
    }

    // Prevents users from messaging themselves.
    @Test
    void sendMessage_toSelf_throwsIllegalArgumentException() {
        MessageSendDto req = new MessageSendDto(sender.getId(), "hi");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);

        assertThatThrownBy(() -> messageService.sendMessage(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot message yourself");
    }

    // Rejects messages to unknown recipients.
    @Test
    void sendMessage_recipientNotFound_throwsNotFound() {
        MessageSendDto req = new MessageSendDto(recipient.getId(), "hi");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // Enforces friendship before allowing direct messages.
    @Test
    void sendMessage_notFriends_throwsForbidden() {
        MessageSendDto req = new MessageSendDto(recipient.getId(), "hi");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(friendshipService.isFriend(sender.getId(), recipient.getId())).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendMessage(req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("only message friends");
    }

    // Reuses existing conversations and updates timestamps on send.
    @Test
    void sendMessage_existingConversation_updatesAndReturnsDto() {
        MessageSendDto req = new MessageSendDto(recipient.getId(), "hi");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(friendshipService.isFriend(sender.getId(), recipient.getId())).thenReturn(true);
        when(conversationRepository.findBetweenUsers(sender.getId(), recipient.getId())).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(conversationRepository.save(conversation)).thenReturn(conversation);

        MessageResDto result = messageService.sendMessage(req);

        assertThat(result.senderId()).isEqualTo(sender.getId());
        assertThat(result.recipientId()).isEqualTo(recipient.getId());
        assertThat(result.conversationId()).isEqualTo(conversation.getId());
    }

    // Creates a new conversation with stable user ordering when none exists.
    @Test
    void sendMessage_noConversation_createsConversation() {
        MessageSendDto req = new MessageSendDto(recipient.getId(), "hi");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(friendshipService.isFriend(sender.getId(), recipient.getId())).thenReturn(true);
        when(conversationRepository.findBetweenUsers(sender.getId(), recipient.getId())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(message.getId());
            return saved;
        });

        MessageResDto result = messageService.sendMessage(req);

        assertThat(result.conversationId()).isEqualTo(11L);
        verify(conversationRepository).save(any(Conversation.class));
    }

    // Rejects access to conversations the user is not part of.
    @Test
    void getConversationMessages_nonMember_throwsForbidden() {
        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserOneId(99L);
        conversation.setUserTwoId(100L);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> messageService.getConversationMessages(conversation.getId(), PageRequest.of(0, 5)))
                .isInstanceOf(ForbiddenException.class);
    }

    // Retrieves messages for a valid conversation member.
    @Test
    void getConversationMessages_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));

        Page<MessageResDto> result = messageService.getConversationMessages(conversation.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).recipientId()).isEqualTo(recipient.getId());
    }

    // Lists conversations sorted by last activity descending.
    @Test
    void getMyConversations_sortedByUpdatedAt() {
        Conversation older = new Conversation();
        older.setId(11L);
        older.setUserOneId(sender.getId());
        older.setUserTwoId(recipient.getId());
        older.setUpdatedAt(LocalDateTime.now().minusHours(2));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(conversationRepository.findByUserOneIdOrUserTwoId(sender.getId(), sender.getId()))
                .thenReturn(List.of(older, conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq(conversation.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq(older.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));

        List<ConversationResDto> result = messageService.getMyConversations();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(conversation.getId());
    }

    // Marks conversation messages as read for the current user.
    @Test
    void markConversationRead_updatesStatuses() {
        MessageReadDto req = new MessageReadDto(conversation.getId());
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        messageService.markConversationRead(req);

        verify(messageRepository).markConversationMessages(conversation.getId(), sender.getId(), MessageStatus.READ);
    }

    // Ensures non-members cannot mark conversations as read.
    @Test
    void markConversationRead_nonMember_throwsForbidden() {
        MessageReadDto req = new MessageReadDto(conversation.getId());
        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserOneId(99L);
        conversation.setUserTwoId(100L);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(sender);
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> messageService.markConversationRead(req))
                .isInstanceOf(ForbiddenException.class);
    }

    // Helper for consistent user setup across tests.
    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
