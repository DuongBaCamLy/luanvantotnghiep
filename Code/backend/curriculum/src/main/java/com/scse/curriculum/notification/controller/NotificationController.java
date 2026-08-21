package com.scse.curriculum.notification.controller;

import com.scse.curriculum.notification.dto.NotificationDto;
import com.scse.curriculum.notification.entity.Notification;
import com.scse.curriculum.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.isCurrentUser(#userId, authentication)")
    public ResponseEntity<List<NotificationDto>> getUserNotifications(@PathVariable Integer userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        List<NotificationDto> dtos = notifications.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.ownsNotification(#id, authentication)")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/user/{userId}/read-all")
    @PreAuthorize("hasRole('ADMIN') or @accessGuard.isCurrentUser(#userId, authentication)")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Integer userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getUser().getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
