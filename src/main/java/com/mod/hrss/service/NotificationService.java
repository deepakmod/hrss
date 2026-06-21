package com.mod.hrss.service;

import com.mod.hrss.dto.response.NotificationDto;

import java.util.List;

public interface NotificationService {
    void sendNotification(Long userId, String title, String message);
    List<NotificationDto> getUnreadNotifications(Long userId);
    List<NotificationDto> getAllNotifications(Long userId);
    void markAsRead(Long notificationId);
}
