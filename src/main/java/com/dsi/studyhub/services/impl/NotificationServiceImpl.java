package com.dsi.studyhub.services.impl;

import com.dsi.studyhub.dtos.NotificationResDto;
import com.dsi.studyhub.entities.Notification;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.exceptions.ForbiddenException;
import com.dsi.studyhub.exceptions.ResourceNotFoundException;
import com.dsi.studyhub.repositories.NotificationRepository;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.AuthenticatedUserService;
import com.dsi.studyhub.services.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Notification creation
    @Override
    @Transactional
    public NotificationResDto createNotification(Long recipientId, String type, String message, String link, Long refId) {
        // Persists and dispatches a realtime notification to the recipient.
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setRefId(refId);

        Notification saved = notificationRepository.save(notification);
        NotificationResDto dto = toDto(saved);

        messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/notifications",
                dto
        );

        return dto;
    }

    // Notification retrieval
    @Override
    @Transactional
    public Page<NotificationResDto> getMyNotifications(int page, int size) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                )
                .map(this::toDto);
    }

    // Read tracking
    @Override
    @Transactional
    public void markRead(Long notificationId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You cannot modify this notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllRead() {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        notificationRepository.markAllRead(currentUser.getId());
    }

    // Unread counts
    @Override
    @Transactional
    public long countUnread() {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        return notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
    }

    // Mapping helper
    private NotificationResDto toDto(Notification notification) {
        return new NotificationResDto(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getLink(),
                notification.getRefId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
