package com.mod.hrss.controller;

import com.mod.hrss.common.ApiResponse;
import com.mod.hrss.dto.response.NotificationDto;
import com.mod.hrss.security.UserPrincipal;
import com.mod.hrss.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUnreadNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved successfully", notifications));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getAllNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        List<NotificationDto> notifications = notificationService.getAllNotifications(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications retrieved successfully", notifications));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Simple safety check done in service (verifies notification exists)
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }
}
