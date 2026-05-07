package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.NotificationResDto;
import org.springframework.data.domain.Page;

public interface NotificationService {
    NotificationResDto createNotification(Long recipientId, String type, String message, String link, Long refId);
    Page<NotificationResDto> getMyNotifications(int page, int size);
    void markRead(Long notificationId);
    void markAllRead();
    long countUnread();
}
