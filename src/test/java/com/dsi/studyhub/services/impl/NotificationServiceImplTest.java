package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.NotificationResDto;
import com.dsi.studyhub.entities.Notification;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.NotificationRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient;
    private Notification notification;

    // Builds shared fixtures to keep each test focused on one behavior.
    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(1L);
        recipient.setUsername("alice");

        notification = new Notification();
        notification.setId(10L);
        notification.setRecipient(recipient);
        notification.setType("LIKE");
        notification.setMessage("Alice liked your post");
        notification.setLink(null);
        notification.setRefId(100L);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }

    // Persists and publishes realtime notifications to the recipient.
    @Test
    void createNotification_savesAndDispatches() {
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResDto result = notificationService.createNotification(
                recipient.getId(), "LIKE", "Alice liked your post", null, 100L);

        assertThat(result.id()).isEqualTo(notification.getId());
        verify(messagingTemplate).convertAndSendToUser(
                eq(recipient.getUsername()), eq("/queue/notifications"), any(NotificationResDto.class));
    }

    // Rejects creation when the recipient does not exist.
    @Test
    void createNotification_missingRecipient_throwsNotFound() {
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createNotification(
                recipient.getId(), "LIKE", "msg", null, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // Returns a page of notifications for the current user.
    @Test
    void getMyNotifications_returnsPage() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(recipient);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(recipient.getId()), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationResDto> result = notificationService.getMyNotifications(0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    // Marks a notification as read for the owner.
    @Test
    void markRead_owner_updatesReadState() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(recipient);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.markRead(notification.getId());

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    // Prevents non-owners from marking notifications as read.
    @Test
    void markRead_nonOwner_throwsForbidden() {
        User other = new User();
        other.setId(2L);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(other);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(notification.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("cannot modify");
    }

    // Skips redundant writes when already read.
    @Test
    void markRead_alreadyRead_noSave() {
        notification.setRead(true);
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(recipient);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationService.markRead(notification.getId());

        verify(notificationRepository, never()).save(notification);
    }

    // Marks all notifications as read for the current user.
    @Test
    void markAllRead_updatesAllForUser() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(recipient);

        notificationService.markAllRead();

        verify(notificationRepository).markAllRead(recipient.getId());
    }

    // Returns unread count for the current user.
    @Test
    void countUnread_returnsCount() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(recipient);
        when(notificationRepository.countByRecipientIdAndIsReadFalse(recipient.getId())).thenReturn(3L);

        long result = notificationService.countUnread();

        assertThat(result).isEqualTo(3L);
    }
}
